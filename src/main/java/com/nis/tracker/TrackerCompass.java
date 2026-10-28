package com.nis.tracker;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;

public final class TrackerCompass {
	private static final String TAG = "manhunt_tracker";

	private TrackerCompass() {}

	public static boolean isTracker(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.COMPASS)) return false;
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return false;
		CompoundTag probe = new CompoundTag();
		probe.putBoolean(TAG, true);
		return data.matchedBy(probe);
	}

	private static ItemStack create() {
		ItemStack stack = new ItemStack(Items.COMPASS);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(TAG, true);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Tracking Compass"));
		return stack;
	}

	/** Guarantees exactly one tracker compass, located in hotbar slot 0. */
	public static void lock(ServerPlayer player) {
		Inventory inv = player.getInventory();
		if (!isTracker(inv.getItem(0))) {
			int found = -1;
			for (int i = 1; i < inv.getContainerSize(); i++) {
				if (isTracker(inv.getItem(i))) { found = i; break; }
			}
			ItemStack displaced = inv.getItem(0).copy();
			if (found >= 0) {
				ItemStack compass = inv.getItem(found);
				inv.setItem(0, compass);
				inv.setItem(found, displaced);
			} else {
				inv.setItem(0, create());
				if (!displaced.isEmpty() && !inv.add(displaced)) {
					player.drop(displaced, false, Prediction.SERVER_ONLY);
				}
			}
		}
		// remove duplicates
		for (int i = 1; i < inv.getContainerSize(); i++) {
			if (isTracker(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
		}
		if (isTracker(player.containerMenu.getCarried())) {
			player.containerMenu.setCarried(ItemStack.EMPTY);
		}
	}

	public static void remove(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (isTracker(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
		}
		if (isTracker(player.containerMenu.getCarried())) {
			player.containerMenu.setCarried(ItemStack.EMPTY);
		}
	}

	/** Writes the lodestone tracker component (1.20.5+ replacement for the old LodestonePos NBT). */
	public static void point(ServerPlayer player, GlobalPos target) {
		ItemStack stack = player.getInventory().getItem(0);
		if (!isTracker(stack)) return;
		// tracked=false -> the compass keeps pointing even though no lodestone block exists there.
		LodestoneTracker tracker = new LodestoneTracker(Optional.of(target), false);
		if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
			stack.set(DataComponents.LODESTONE_TRACKER, tracker);
		}
	}
}

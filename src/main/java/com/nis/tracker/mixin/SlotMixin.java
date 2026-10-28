package com.nis.tracker.mixin;

import com.nis.tracker.TrackerCompass;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The tracker compass may only live in the player's own inventory. Every container interaction
 * (click, shift-click, hotkey swap, drag) validates through Slot#mayPlace, so refusing here blocks
 * chests, ender chests, hoppers, crafting grids, etc.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
	@Shadow
	public Container container;

	@Inject(method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void manhunt$compassStaysInInventory(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!(this.container instanceof Inventory) && TrackerCompass.isTracker(stack)) {
			cir.setReturnValue(false);
		}
	}
}

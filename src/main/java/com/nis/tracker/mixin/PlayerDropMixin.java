package com.nis.tracker.mixin;

import com.nis.tracker.TrackerCompass;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric API has no ItemDropCallback, so this is the equivalent: every way a player can drop an item
 * (Q key, dragging out of the inventory, death drops via Inventory#dropAll) funnels through this method.
 * Returning null cancels the drop; TrackerCompass#lock re-supplies the compass to slot 0 on the next tick.
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerDropMixin {
	@Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At("HEAD"), cancellable = true)
	private void manhunt$cancelCompassDrop(ItemStack stack, boolean randomly, Prediction prediction,
			CallbackInfoReturnable<ItemEntity> cir) {
		if (TrackerCompass.isTracker(stack)) {
			cir.setReturnValue(null);
		}
	}
}

package com.nis.tracker.mixin;

import com.nis.tracker.TrackerCompass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * createItemStackToDrop is declared directly on LivingEntity (Player does not override it).
 * Intercepting here prevents the tracking compass from spawning as an ItemEntity during death drops (Inventory#dropAll).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDropMixin {
	@Inject(method = "createItemStackToDrop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At("HEAD"), cancellable = true)
	private void manhunt$noCompassEntity(ItemStack stack, boolean randomly, boolean thrownFromHand,
			CallbackInfoReturnable<ItemEntity> cir) {
		if (TrackerCompass.isTracker(stack)) {
			cir.setReturnValue(null);
		}
	}
}

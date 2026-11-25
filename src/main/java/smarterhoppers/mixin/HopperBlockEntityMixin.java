package smarterhoppers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import smarterhoppers.rules.LineRouting;
import smarterhoppers.tower.TowerRouting;

/**
 * Hooks run at the head of the hopper methods, before Lithium's own injections, so they behave
 * the same with and without Lithium.
 */
@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityMixin {
	@Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
	private static void smarterHoppers$routeEject(Level level, BlockPos pos, HopperBlockEntity self,
			CallbackInfoReturnable<Boolean> cir) {
		if (LineRouting.tryInsertBelow(level, pos, self)) {
			cir.setReturnValue(true);
			return;
		}
		Boolean towerResult = TowerRouting.eject(level, pos, self);
		if (towerResult != null) {
			cir.setReturnValue(towerResult);
		}
	}
}

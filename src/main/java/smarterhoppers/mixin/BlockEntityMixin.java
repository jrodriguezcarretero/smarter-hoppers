package smarterhoppers.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import smarterhoppers.marker.FastItemFramesCompat;
import smarterhoppers.marker.Markers;
import smarterhoppers.tower.HopperWake;

/**
 * A block entity changed. If it is a Fast Item Frames frame block, the neighbouring container may
 * have gained or lost its marker; if it is a storage container, hoppers feeding it may now decide
 * differently (see {@link HopperWake}).
 */
@Mixin(BlockEntity.class)
abstract class BlockEntityMixin {
	@Shadow
	protected @Nullable Level level;

	@Shadow
	@Final
	protected BlockPos worldPosition;

	@Inject(method = "setChanged()V", at = @At("HEAD"))
	private void smarterHoppers$onChanged(CallbackInfo ci) {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}
		BlockEntity self = (BlockEntity) (Object) this;
		if (FastItemFramesCompat.isFrameBlockEntity(self)) {
			Markers.invalidateAround(this.level, this.worldPosition);
		} else if (self instanceof Container && !(self instanceof HopperBlockEntity)) {
			HopperWake.containerChanged(this.level, this.worldPosition);
		}
	}
}

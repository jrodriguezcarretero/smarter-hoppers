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
import net.minecraft.world.level.block.entity.BlockEntity;

import smarterhoppers.marker.FastItemFramesCompat;
import smarterhoppers.marker.Markers;

/** A Fast Item Frames frame block changed its item: the neighbouring container may have gained or lost its marker. */
@Mixin(BlockEntity.class)
abstract class BlockEntityMixin {
	@Shadow
	protected @Nullable Level level;

	@Shadow
	@Final
	protected BlockPos worldPosition;

	@Inject(method = "setChanged()V", at = @At("HEAD"))
	private void smarterHoppers$invalidateMarker(CallbackInfo ci) {
		if (this.level != null && !this.level.isClientSide()
				&& FastItemFramesCompat.isFrameBlockEntity((BlockEntity) (Object) this)) {
			Markers.invalidateAround(this.level, this.worldPosition);
		}
	}
}

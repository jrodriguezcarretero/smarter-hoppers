package smarterhoppers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

import smarterhoppers.marker.Markers;

/** A vanilla item frame changed its item: the container it hangs on may have gained or lost its marker. */
@Mixin(ItemFrame.class)
abstract class ItemFrameMixin {
	@Inject(method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V", at = @At("TAIL"))
	private void smarterHoppers$invalidateMarker(ItemStack itemStack, boolean updateNeighbours, CallbackInfo ci) {
		ItemFrame self = (ItemFrame) (Object) this;
		// pos is still null while the frame is being created or loaded
		if (!self.level().isClientSide() && self.getPos() != null) {
			Markers.invalidate(self.level(), Markers.supportOf(self));
		}
	}
}

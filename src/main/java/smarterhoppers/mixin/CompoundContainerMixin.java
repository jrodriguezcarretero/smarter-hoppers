package smarterhoppers.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Double chests: vanilla already forwards {@code canPlaceItem} to the right half, but not
 * {@code canTakeItem}, which would skip R1. Forward it the same way.
 */
@Mixin(CompoundContainer.class)
abstract class CompoundContainerMixin {
	@Shadow
	@Final
	private Container container1;

	@Shadow
	@Final
	private Container container2;

	public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
		int firstSize = this.container1.getContainerSize();
		return slot >= firstSize
				? this.container2.canTakeItem(into, slot - firstSize, itemStack)
				: this.container1.canTakeItem(into, slot, itemStack);
	}
}

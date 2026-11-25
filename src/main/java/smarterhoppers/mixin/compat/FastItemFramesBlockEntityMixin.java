package smarterhoppers.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ListBackedContainer;

import smarterhoppers.marker.Markers;

/**
 * Fast Item Frames' frame block is a container with no restrictions, so a hopper under it would
 * pull the comparator out, and a hopper facing it could push items in. Lock frames holding a marker.
 * Only applied when Fast Item Frames is installed.
 */
@Pseudo
@Mixin(targets = "fuzs.fastitemframes.common.world.level.block.entity.ItemFrameBlockEntity")
abstract class FastItemFramesBlockEntityMixin {
	/** Replaces {@code ListBackedContainer#canPlaceItem}'s default. */
	public boolean canPlaceItem(int slot, ItemStack itemStack) {
		Container self = (Container) this;
		if (Markers.isMarkerFrameContainer(self)) {
			return false;
		}
		ItemStack current = self.getItem(slot);
		return ((ListBackedContainer) this).acceptsItemType(itemStack)
				&& (current.isEmpty() || current.getCount() < self.getMaxStackSize(itemStack));
	}

	/** Replaces {@code Container#canTakeItem}'s default. */
	public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
		return !Markers.isMarkerFrameContainer((Container) this);
	}
}

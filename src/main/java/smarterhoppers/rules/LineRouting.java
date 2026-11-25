package smarterhoppers.rules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import smarterhoppers.marker.Markers;
import smarterhoppers.util.Loaded;

/**
 * R3: a hopper of the sorting line (not facing down) with a marked container right below it puts
 * items into that container first. Whatever the marked container does not take (R2) carries on in
 * the hopper's facing direction as usual.
 */
public final class LineRouting {
	private LineRouting() {
	}

	/** Tries to move one item into a marked container below {@code hopper}; true if one moved. */
	public static boolean tryInsertBelow(Level level, BlockPos pos, HopperBlockEntity hopper) {
		if (!(level instanceof ServerLevel serverLevel)
				|| hopper.getBlockState().getValue(HopperBlock.FACING) == Direction.DOWN) {
			return false;
		}
		BlockPos belowPos = pos.below();
		if (!(Loaded.blockEntity(level, belowPos) instanceof Container) || !Markers.isMarked(serverLevel, belowPos)) {
			return false;
		}
		Container below = HopperBlockEntity.getContainerAt(level, belowPos);
		if (below == null) {
			return false;
		}
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			ItemStack itemStack = hopper.getItem(slot);
			if (itemStack.isEmpty()) {
				continue;
			}
			int originalCount = itemStack.getCount();
			ItemStack rest = HopperBlockEntity.addItem(hopper, below, hopper.removeItem(slot, 1), Direction.UP);
			if (rest.isEmpty()) {
				below.setChanged();
				return true;
			}
			// Same restore as vanilla's ejectItems.
			itemStack.setCount(originalCount);
			if (originalCount == 1) {
				hopper.setItem(slot, itemStack);
			}
		}
		return false;
	}
}

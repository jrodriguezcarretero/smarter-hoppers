package smarterhoppers.tower;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Routing inside storage towers (see {@link Towers}), for hoppers with two outputs:
 * <ul>
 * <li>T1: the hopper only pushes an item into its chest if the chest already holds it.</li>
 * <li>T2: the hopper below only pulls what the chest will not take.</li>
 * <li>T3: and only if some chest further down holds the item and has room for it.</li>
 * </ul>
 * Items with no home in the column (no chest of it holds them) move as in vanilla, so towers of
 * empty, unsorted chests keep working. Items whose home is full wait in the hopper instead of
 * filling another chest.
 */
public final class TowerRouting {
	private TowerRouting() {
	}

	/** T1. Replaces {@code ejectItems} for tower hoppers; null for any other hopper. */
	public static @Nullable Boolean eject(Level level, BlockPos pos, HopperBlockEntity hopper) {
		BlockState state = hopper.getBlockState();
		Container chest = Towers.towerChest(level, pos, state);
		if (chest == null) {
			return null;
		}
		Direction face = state.getValue(HopperBlock.FACING).getOpposite();
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			ItemStack itemStack = hopper.getItem(slot);
			if (itemStack.isEmpty()
					|| Towers.hasHome(level, pos, itemStack) && !Towers.contains(chest, itemStack)) {
				continue;
			}
			int originalCount = itemStack.getCount();
			ItemStack rest = HopperBlockEntity.addItem(hopper, chest, hopper.removeItem(slot, 1), face);
			if (rest.isEmpty()) {
				chest.setChanged();
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

	/** T2 and T3, for {@code canTakeItem} of a hopper when {@code into} pulls from it. */
	public static boolean mayPassDown(HopperBlockEntity self, Container into, ItemStack stack) {
		Level level = self.getLevel();
		BlockPos pos = self.getBlockPos();
		if (level == null || level.isClientSide()
				|| !(into instanceof HopperBlockEntity below) || !below.getBlockPos().equals(pos.below())) {
			return true;
		}
		BlockState state = self.getBlockState();
		Container chest = Towers.towerChest(level, pos, state);
		if (chest == null || !Towers.hasHome(level, pos, stack)) {
			return true;
		}
		Direction face = state.getValue(HopperBlock.FACING).getOpposite();
		if (Towers.contains(chest, stack) && Towers.hasRoom(chest, stack, face)) {
			return false;
		}
		return Towers.canStore(level, pos.below(), stack);
	}
}

package smarterhoppers.tower;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Storage towers: a vertical chain of hoppers, each facing sideways into a chest (any non-hopper
 * container). A hopper of the chain with another hopper right below has two outputs: its chest,
 * and the hopper below, which pulls from it. The chests of a column are the ones faced by the
 * hoppers of the chain from a given hopper downwards.
 */
public final class Towers {
	/** Longest hopper chain looked at, to bound the cost of a lookup. */
	static final int MAX_DEPTH = 32;

	private Towers() {
	}

	/** The container a hopper at {@code pos} faces, if it is a block container other than a hopper. */
	public static @Nullable Container facedChest(Level level, BlockPos pos, BlockState hopperState) {
		Direction facing = hopperState.getValue(HopperBlock.FACING);
		BlockPos target = pos.relative(facing);
		BlockEntity blockEntity = level.getBlockEntity(target);
		if (!(blockEntity instanceof Container) || blockEntity instanceof HopperBlockEntity) {
			return null;
		}
		return HopperBlockEntity.getContainerAt(level, target);
	}

	/** The chest of a tower hopper (two outputs), or null if the hopper at {@code pos} is not one. */
	public static @Nullable Container towerChest(Level level, BlockPos pos, BlockState hopperState) {
		if (hopperState.getValue(HopperBlock.FACING) == Direction.DOWN
				|| !(level.getBlockEntity(pos.below()) instanceof HopperBlockEntity)) {
			return null;
		}
		return facedChest(level, pos, hopperState);
	}

	/** Whether some chest of the column starting at hopper {@code pos} already holds {@code stack}'s item. */
	public static boolean hasHome(Level level, BlockPos pos, ItemStack stack) {
		BlockPos current = pos;
		for (int depth = 0; depth < MAX_DEPTH; depth++) {
			if (!(level.getBlockEntity(current) instanceof HopperBlockEntity hopper)) {
				return false;
			}
			Container chest = facedChest(level, current, hopper.getBlockState());
			if (chest != null && contains(chest, stack)) {
				return true;
			}
			current = current.below();
		}
		return false;
	}

	/** Whether some chest of the column starting at hopper {@code pos} holds {@code stack}'s item and has room for it. */
	public static boolean canStore(Level level, BlockPos pos, ItemStack stack) {
		BlockPos current = pos;
		for (int depth = 0; depth < MAX_DEPTH; depth++) {
			if (!(level.getBlockEntity(current) instanceof HopperBlockEntity hopper)) {
				return false;
			}
			BlockState state = hopper.getBlockState();
			Container chest = facedChest(level, current, state);
			if (chest != null && contains(chest, stack)
					&& hasRoom(chest, stack, state.getValue(HopperBlock.FACING).getOpposite())) {
				return true;
			}
			current = current.below();
		}
		return false;
	}

	public static boolean contains(Container container, ItemStack stack) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (ItemStack.isSameItemSameComponents(container.getItem(slot), stack)) {
				return true;
			}
		}
		return false;
	}

	/** Whether one more of {@code stack}'s item fits, inserting from side {@code face}. */
	public static boolean hasRoom(Container container, ItemStack stack, Direction face) {
		if (container instanceof WorldlyContainer worldly) {
			for (int slot : worldly.getSlotsForFace(face)) {
				if (worldly.canPlaceItemThroughFace(slot, stack, face) && fits(container, slot, stack)) {
					return true;
				}
			}
			return false;
		}
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (fits(container, slot, stack)) {
				return true;
			}
		}
		return false;
	}

	private static boolean fits(Container container, int slot, ItemStack stack) {
		if (!container.canPlaceItem(slot, stack)) {
			return false;
		}
		ItemStack current = container.getItem(slot);
		return current.isEmpty()
				|| ItemStack.isSameItemSameComponents(current, stack)
						&& current.getCount() < Math.min(current.getMaxStackSize(), container.getMaxStackSize(current));
	}
}

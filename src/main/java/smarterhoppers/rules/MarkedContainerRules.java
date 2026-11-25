package smarterhoppers.rules;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import smarterhoppers.marker.Markers;
import smarterhoppers.tower.TowerCapacity;
import smarterhoppers.util.Loaded;

/**
 * Rules for marked containers (see {@link Markers}):
 * <ul>
 * <li>R1: an item can only be taken out while more than {@link #MIN_KEPT} of it remain, so the
 * filter items always stay.</li>
 * <li>R2: an item can only be put in if the container already holds it.</li>
 * <li>R4: if the container feeds a tower, only if the tower can store it too
 * ({@link TowerCapacity}).</li>
 * </ul>
 * Counts cover the whole container, so both halves of a double chest.
 */
public final class MarkedContainerRules {
	/** Copies of each item a marked container always keeps. */
	public static final int MIN_KEPT = 2;

	private MarkedContainerRules() {
	}

	/** R2 and R4, for {@code Container#canPlaceItem} of a container block entity. */
	public static boolean mayInsert(BlockEntity self, ItemStack stack) {
		if (!(self.getLevel() instanceof ServerLevel level) || !Markers.isMarked(level, self.getBlockPos())) {
			return true;
		}
		if (count(level, self, stack, 1) == 0) {
			return false;
		}
		BlockPos pos = self.getBlockPos();
		return !TowerCapacity.feedsTower(level, pos)
				|| TowerCapacity.canAccept(level, pos, parts(level, self), stack, MIN_KEPT);
	}

	/** R1, for {@code Container#canTakeItem} of a container block entity. */
	public static boolean mayExtract(BlockEntity self, ItemStack stack) {
		if (!(self.getLevel() instanceof ServerLevel level) || !Markers.isMarked(level, self.getBlockPos())) {
			return true;
		}
		return count(level, self, stack, MIN_KEPT + 1) > MIN_KEPT;
	}

	/** Counts {@code stack}'s item in the whole container, stopping once {@code enough} is reached. */
	private static int count(ServerLevel level, BlockEntity self, ItemStack stack, int enough) {
		int total = 0;
		for (Container part : parts(level, self)) {
			for (int slot = 0; slot < part.getContainerSize(); slot++) {
				ItemStack inSlot = part.getItem(slot);
				if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
					total += inSlot.getCount();
					if (total >= enough) {
						return total;
					}
				}
			}
		}
		return total;
	}

	/** The block entity's container plus, for a double chest, the other half. */
	private static List<Container> parts(ServerLevel level, BlockEntity self) {
		Container own = (Container) self;
		BlockState state = self.getBlockState();
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			BlockPos otherPos = ChestBlock.getConnectedBlockPos(self.getBlockPos(), state);
			if (level.isLoaded(otherPos) && Loaded.blockEntity(level, otherPos) instanceof Container other) {
				return List.of(own, other);
			}
		}
		return List.of(own);
	}
}

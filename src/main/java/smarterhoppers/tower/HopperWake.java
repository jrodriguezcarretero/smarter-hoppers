package smarterhoppers.tower;

import java.util.ArrayDeque;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import smarterhoppers.compat.LithiumCompat;
import smarterhoppers.marker.Markers;
import smarterhoppers.util.Loaded;

/**
 * When a storage container changes, the decisions of the hoppers that feed it may change too: room
 * freed in a tower chest lets items waiting further up move on (T3, R4), and a new marker lets the
 * line hopper above start feeding it (R3). Walks those hoppers upstream and asks Lithium to retry them.
 * Without Lithium, hoppers retry every time anyway, so this does nothing.
 */
public final class HopperWake {
	/** Most hoppers woken per change, to bound the cost. */
	static final int MAX_HOPPERS = 64;

	private HopperWake() {
	}

	/** {@code pos} holds a container (not a hopper) whose contents or marker changed. */
	public static void containerChanged(Level level, BlockPos pos) {
		if (!LithiumCompat.LOADED || !(level instanceof ServerLevel) || !level.isLoaded(pos)) {
			return;
		}
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		addFeeders(level, pos, queue);
		BlockState state = Loaded.blockState(level, pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			addFeeders(level, ChestBlock.getConnectedBlockPos(pos, state), queue);
		}
		if (queue.isEmpty()) {
			return;
		}
		LongOpenHashSet seen = new LongOpenHashSet();
		while (!queue.isEmpty() && seen.size() < MAX_HOPPERS) {
			BlockPos hopperPos = queue.poll();
			if (!seen.add(hopperPos.asLong()) || !(Loaded.blockEntity(level, hopperPos) instanceof HopperBlockEntity hopper)) {
				continue;
			}
			LithiumCompat.retry(hopper);
			// Upstream: the hopper above (it is pulled from by this one), hoppers facing this one...
			BlockPos above = hopperPos.above();
			BlockEntity aboveEntity = Loaded.blockEntity(level, above);
			if (aboveEntity instanceof HopperBlockEntity) {
				queue.add(above);
			} else if (aboveEntity instanceof Container && level instanceof ServerLevel serverLevel
					&& Markers.isMarked(serverLevel, above)) {
				// ...and through a marked container, the line hopper feeding it.
				LithiumCompat.retry((Container) aboveEntity);
				addFeeders(level, above, queue);
			}
			addFacing(level, hopperPos, queue);
		}
	}

	/** Hoppers facing {@code pos}, plus the hopper right above it (a line hopper over a marked container). */
	private static void addFeeders(Level level, BlockPos pos, ArrayDeque<BlockPos> queue) {
		addFacing(level, pos, queue);
		if (Loaded.blockEntity(level, pos.above()) instanceof HopperBlockEntity) {
			queue.add(pos.above());
		}
	}

	private static void addFacing(Level level, BlockPos pos, ArrayDeque<BlockPos> queue) {
		for (Direction direction : Direction.values()) {
			if (direction == Direction.DOWN) {
				continue; // a hopper below would have to face up
			}
			BlockPos neighbour = pos.relative(direction);
			BlockState state = Loaded.blockState(level, neighbour);
			if (state.getBlock() instanceof HopperBlock && state.getValue(HopperBlock.FACING) == direction.getOpposite()) {
				queue.add(neighbour);
			}
		}
	}
}

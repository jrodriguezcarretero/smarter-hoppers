package smarterhoppers.marker;

import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

/**
 * Marked containers: a container with an item frame holding a comparator on any face,
 * either a vanilla item frame entity or a Fast Item Frames frame block. For a double chest,
 * a frame on either half marks the whole chest.
 *
 * <p>Results are cached per block position. Entries are dropped when a frame next to the
 * position changes (see {@link #invalidateAround}) and expire after {@link #TTL_TICKS} as a
 * safety net.
 */
public final class Markers {
	static final long TTL_TICKS = 100;
	private static final long NOT_CACHED = Long.MIN_VALUE;

	/** Per level: block position -> (expiry game time << 1 | marked bit). */
	private static final Map<Level, Long2LongOpenHashMap> CACHE = new WeakHashMap<>();

	private Markers() {
	}

	public static boolean isMarker(ItemStack framed) {
		return framed.is(Items.COMPARATOR);
	}

	/** Whether the container at {@code pos} (either half, for a double chest) is marked. */
	public static boolean isMarked(ServerLevel level, BlockPos pos) {
		if (isHalfMarked(level, pos)) {
			return true;
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return isHalfMarked(level, ChestBlock.getConnectedBlockPos(pos, state));
		}
		return false;
	}

	/**
	 * Whether {@code container} is an item frame that holds a marker (Fast Item Frames block form).
	 * Hoppers must never move items in or out of these.
	 */
	public static boolean isMarkerFrameContainer(Container container) {
		return container instanceof BlockEntity blockEntity
				&& FastItemFramesCompat.isFrameBlockEntity(blockEntity)
				&& isMarker(FastItemFramesCompat.framedItem(blockEntity));
	}

	private static boolean isHalfMarked(ServerLevel level, BlockPos pos) {
		long now = level.getGameTime();
		Long2LongOpenHashMap cache = CACHE.computeIfAbsent(level, l -> {
			Long2LongOpenHashMap map = new Long2LongOpenHashMap();
			map.defaultReturnValue(NOT_CACHED);
			return map;
		});
		long key = pos.asLong();
		long entry = cache.get(key);
		if (entry != NOT_CACHED && (entry >> 1) > now) {
			return (entry & 1) != 0;
		}
		boolean marked = scan(level, pos);
		cache.put(key, ((now + TTL_TICKS) << 1) | (marked ? 1 : 0));
		return marked;
	}

	private static boolean scan(ServerLevel level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos framePos = pos.relative(direction);
			BlockState frameState = level.getBlockState(framePos);
			if (FastItemFramesCompat.towardsSupport(frameState).filter(d -> d == direction.getOpposite()).isPresent()) {
				BlockEntity blockEntity = level.getBlockEntity(framePos);
				if (blockEntity != null && isMarker(FastItemFramesCompat.framedItem(blockEntity))) {
					return true;
				}
			}
		}
		for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, new AABB(pos).inflate(0.5))) {
			if (frame.isAlive() && supportOf(frame).equals(pos) && isMarker(frame.getItem())) {
				return true;
			}
		}
		return false;
	}

	public static BlockPos supportOf(ItemFrame frame) {
		return frame.getPos().relative(frame.getDirection().getOpposite());
	}

	/** Forget cached results for {@code pos}. */
	public static void invalidate(Level level, BlockPos pos) {
		Long2LongOpenHashMap cache = CACHE.get(level);
		if (cache != null) {
			cache.remove(pos.asLong());
		}
	}

	/** Forget cached results for every block next to {@code framePos}. */
	public static void invalidateAround(Level level, BlockPos framePos) {
		Long2LongOpenHashMap cache = CACHE.get(level);
		if (cache != null && !cache.isEmpty()) {
			for (Direction direction : Direction.values()) {
				cache.remove(framePos.relative(direction).asLong());
			}
		}
	}

	public static void clear(Level level) {
		CACHE.remove(level);
	}
}

package smarterhoppers.tower;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * R4: a marked container that feeds a tower (has a hopper right below it) only takes an item if the
 * tower can store it, counting what is already on its way.
 *
 * <p>The tower is every hopper reachable from the hopper below the marked container (the one each
 * hopper faces, if it is a hopper, and the one below it, which pulls from it), and the chests those
 * hoppers face. The check simulates storing everything in transit (in the marked container beyond
 * its kept filter items, and in the tower's hoppers) into the chests that already hold each item,
 * sharing each chest's empty slots between all its items, and then asks whether one more fits.
 * Items no chest of the tower holds move as in vanilla inside the tower (see {@link TowerRouting}),
 * so they count against the empty slots of any chest.
 */
public final class TowerCapacity {
	/** Groups stacks of the same item with the same components, whatever their count. */
	private static final Hash.Strategy<ItemStack> ITEM_AND_COMPONENTS = new Hash.Strategy<>() {
		@Override
		public int hashCode(ItemStack stack) {
			return stack == null ? 0 : ItemStack.hashItemAndComponents(stack);
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			return a == b || a != null && b != null && ItemStack.isSameItemSameComponents(a, b);
		}
	};

	/** Most hoppers looked at, to bound the cost of a check. */
	static final int MAX_HOPPERS = 64;

	/** Per level: marked container position -> result of the last check this tick. */
	private static final Map<Level, Long2ObjectOpenHashMap<Memo>> MEMO = new WeakHashMap<>();

	private record Memo(long gameTime, ItemStack item, boolean result) {
	}

	private TowerCapacity() {
	}

	/** Whether the marked container at {@code markedPos} feeds a tower. */
	public static boolean feedsTower(Level level, BlockPos markedPos) {
		return level.getBlockEntity(markedPos.below()) instanceof HopperBlockEntity;
	}

	/**
	 * Whether the tower below {@code marked} can store one more {@code stack}. {@code minKept}
	 * copies of each item in {@code marked} are filter items, not in transit.
	 */
	public static boolean canAccept(Level level, BlockPos markedPos, List<Container> marked, ItemStack stack, int minKept) {
		long now = level.getGameTime();
		Long2ObjectOpenHashMap<Memo> memo = MEMO.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>());
		Memo last = memo.get(markedPos.asLong());
		// canPlaceItem asks once per slot; only simulate once per item and tick.
		if (last != null && last.gameTime == now && ItemStack.isSameItemSameComponents(last.item, stack)) {
			return last.result;
		}
		boolean result = simulate(level, markedPos, marked, stack, minKept);
		memo.put(markedPos.asLong(), new Memo(now, stack.copyWithCount(1), result));
		return result;
	}

	/** Forget the last check for {@code markedPos}, e.g. after its contents changed. */
	public static void forget(Level level, BlockPos markedPos) {
		Long2ObjectOpenHashMap<Memo> memo = MEMO.get(level);
		if (memo != null) {
			memo.remove(markedPos.asLong());
		}
	}

	private static boolean simulate(Level level, BlockPos markedPos, List<Container> marked, ItemStack stack, int minKept) {
		List<Chest> chests = new ArrayList<>();
		Object2IntOpenCustomHashMap<ItemStack> inTransit = new Object2IntOpenCustomHashMap<>(ITEM_AND_COMPONENTS);
		collect(level, markedPos.below(), chests, inTransit);
		if (chests.isEmpty()) {
			return false;
		}

		Object2IntOpenCustomHashMap<ItemStack> inMarked = new Object2IntOpenCustomHashMap<>(ITEM_AND_COMPONENTS);
		for (Container part : marked) {
			addAll(part, inMarked);
		}
		for (Object2IntMap.Entry<ItemStack> entry : inMarked.object2IntEntrySet()) {
			int transit = entry.getIntValue() - minKept;
			if (transit > 0) {
				inTransit.addTo(entry.getKey(), transit);
			}
		}

		for (Object2IntMap.Entry<ItemStack> entry : inTransit.object2IntEntrySet()) {
			store(chests, entry.getKey(), entry.getIntValue());
		}
		return store(chests, stack, 1) == 0;
	}

	/** Stores {@code count} of {@code stack}'s item where the tower would route it; returns what does not fit. */
	private static int store(List<Chest> chests, ItemStack stack, int count) {
		boolean hasHome = false;
		for (Chest chest : chests) {
			hasHome |= chest.holds(stack);
		}
		for (Chest chest : chests) {
			if (count == 0) {
				break;
			}
			if (!hasHome || chest.holds(stack)) {
				count = chest.store(stack, count);
			}
		}
		return count;
	}

	/** Walks the tower from {@code start}, collecting its chests and the items in its hoppers. */
	private static void collect(Level level, BlockPos start, List<Chest> chests, Object2IntOpenCustomHashMap<ItemStack> inTransit) {
		LongOpenHashSet seenHoppers = new LongOpenHashSet();
		LongOpenHashSet seenChests = new LongOpenHashSet();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		while (!queue.isEmpty() && seenHoppers.size() < MAX_HOPPERS) {
			BlockPos pos = queue.poll();
			if (!seenHoppers.add(pos.asLong()) || !(level.getBlockEntity(pos) instanceof HopperBlockEntity hopper)) {
				continue;
			}
			addAll(hopper, inTransit);
			Direction facing = hopper.getBlockState().getValue(HopperBlock.FACING);
			BlockPos target = pos.relative(facing);
			BlockEntity targetEntity = level.getBlockEntity(target);
			if (targetEntity instanceof HopperBlockEntity) {
				queue.add(target);
			} else if (targetEntity instanceof Container && seenChests.add(chestKey(level, target))) {
				Container chest = HopperBlockEntity.getContainerAt(level, target);
				if (chest != null) {
					chests.add(new Chest(chest));
				}
			}
			if (level.getBlockEntity(pos.below()) instanceof HopperBlockEntity) {
				queue.add(pos.below());
			}
		}
	}

	/** Same key for both halves of a double chest. */
	private static long chestKey(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return Math.min(pos.asLong(), ChestBlock.getConnectedBlockPos(pos, state).asLong());
		}
		return pos.asLong();
	}

	private static void addAll(Container container, Object2IntOpenCustomHashMap<ItemStack> counts) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack inSlot = container.getItem(slot);
			if (!inSlot.isEmpty()) {
				counts.addTo(inSlot.copyWithCount(1), inSlot.getCount());
			}
		}
	}

	/** Free space of one chest: room left in the stacks of each item it holds, plus shared empty slots. */
	private static final class Chest {
		private final Object2IntOpenCustomHashMap<ItemStack> room = new Object2IntOpenCustomHashMap<>(ITEM_AND_COMPONENTS);
		private final int maxStackSize;
		private int emptySlots;

		Chest(Container container) {
			this.maxStackSize = container.getMaxStackSize();
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack inSlot = container.getItem(slot);
				if (inSlot.isEmpty()) {
					this.emptySlots++;
				} else {
					this.room.addTo(inSlot.copyWithCount(1), Math.max(0, limit(inSlot) - inSlot.getCount()));
				}
			}
		}

		private int limit(ItemStack stack) {
			return Math.min(stack.getMaxStackSize(), this.maxStackSize);
		}

		boolean holds(ItemStack stack) {
			return this.room.containsKey(stack);
		}

		/** Stores up to {@code count} of {@code stack}'s item; returns what is left. */
		int store(ItemStack stack, int count) {
			int room = this.room.getInt(stack);
			int stored = Math.min(room, count);
			count -= stored;
			if (stored > 0) {
				this.room.put(stack.copyWithCount(1), room - stored);
			}
			while (count > 0 && this.emptySlots > 0) {
				this.emptySlots--;
				int perSlot = limit(stack);
				int inSlot = Math.min(perSlot, count);
				count -= inSlot;
				this.room.addTo(stack.copyWithCount(1), perSlot - inSlot);
			}
			return count;
		}
	}
}

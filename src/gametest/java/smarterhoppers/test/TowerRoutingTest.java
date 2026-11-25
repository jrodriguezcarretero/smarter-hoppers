package smarterhoppers.test;

import static smarterhoppers.test.TestSetup.container;
import static smarterhoppers.test.TestSetup.count;
import static smarterhoppers.test.TestSetup.expectCount;
import static smarterhoppers.test.TestSetup.hopper;
import static smarterhoppers.test.TestSetup.put;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Phase 5: routing inside a tower. Four hoppers stacked at x=1 (T1 on top), each facing east into
 * a barrel at x=2 (C4 on top). C1/C2 hold logs, C3 stairs, C4 fences, like a wood tower.
 */
public class TowerRoutingTest {
	static final BlockPos T1 = new BlockPos(1, 4, 1);
	static final BlockPos T2 = T1.below();
	static final BlockPos T3 = T2.below();
	static final BlockPos T4 = T3.below();
	static final BlockPos C4 = T1.east();
	static final BlockPos C3 = T2.east();
	static final BlockPos C2 = T3.east();
	static final BlockPos C1 = T4.east();
	private static final int SETTLE = 200;

	static void buildWoodTower(GameTestHelper helper) {
		for (BlockPos chest : new BlockPos[] {C1, C2, C3, C4}) {
			helper.setBlock(chest, Blocks.BARREL);
		}
		put(helper, C1, 0, Items.OAK_LOG, 1);
		put(helper, C2, 0, Items.OAK_LOG, 1);
		put(helper, C3, 0, Items.OAK_STAIRS, 1);
		put(helper, C4, 0, Items.OAK_FENCE, 1);
		for (BlockPos hopper : new BlockPos[] {T1, T2, T3, T4}) {
			hopper(helper, hopper, Direction.EAST);
		}
	}

	static void fill(GameTestHelper helper, BlockPos chest, ItemStack stack) {
		Container container = container(helper, chest);
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			container.setItem(slot, stack.copy());
		}
	}

	@GameTest(maxTicks = 400)
	public void itemsReachTheChestThatHoldsThem(GameTestHelper helper) {
		buildWoodTower(helper);
		put(helper, T1, 0, Items.OAK_LOG, 4);
		put(helper, T1, 1, Items.OAK_STAIRS, 3);
		put(helper, T1, 2, Items.OAK_FENCE, 2);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, C4, Items.OAK_FENCE, 3, "fences in C4");
			expectCount(helper, C3, Items.OAK_STAIRS, 4, "stairs in C3");
			helper.assertTrue(count(helper, C1, Items.OAK_LOG) + count(helper, C2, Items.OAK_LOG) == 6,
					Component.literal("all logs in C1/C2"));
			for (BlockPos chest : new BlockPos[] {C3, C4}) {
				expectCount(helper, chest, Items.OAK_LOG, 0, "no logs in " + chest);
			}
			expectCount(helper, C4, Items.OAK_STAIRS, 0, "no stairs in C4");
			for (BlockPos chest : new BlockPos[] {C1, C2, C3}) {
				expectCount(helper, chest, Items.OAK_FENCE, 0, "no fences in " + chest);
			}
			helper.succeed();
		});
	}

	/** The original problem 2: log chests full, more logs arrive, then stairs. */
	@GameTest(maxTicks = 400)
	public void fullHomeDoesNotBlockOtherItems(GameTestHelper helper) {
		buildWoodTower(helper);
		fill(helper, C1, new ItemStack(Items.OAK_LOG, 64));
		fill(helper, C2, new ItemStack(Items.OAK_LOG, 64));
		put(helper, T1, 0, Items.OAK_LOG, 3);
		put(helper, T1, 1, Items.OAK_STAIRS, 2);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, C3, Items.OAK_STAIRS, 3, "stairs still reach C3");
			expectCount(helper, T1, Items.OAK_LOG, 3, "extra logs wait in T1");
			for (BlockPos chest : new BlockPos[] {C3, C4}) {
				expectCount(helper, chest, Items.OAK_LOG, 0, "no logs spilled into " + chest);
			}
			helper.succeed();
		});
	}

	/** Items with no home in the tower move as in vanilla, so they end up in some chest. */
	@GameTest(maxTicks = 400)
	public void homelessItemsMoveAsInVanilla(GameTestHelper helper) {
		buildWoodTower(helper);
		put(helper, T1, 0, Items.DIAMOND, 2);
		helper.runAfterDelay(SETTLE, () -> {
			int stored = 0;
			for (BlockPos chest : new BlockPos[] {C1, C2, C3, C4}) {
				stored += count(helper, chest, Items.DIAMOND);
			}
			helper.assertTrue(stored == 2, Component.literal("diamonds stored: " + stored));
			helper.succeed();
		});
	}

	/**
	 * Hopper sorter: line hopper facing the next line hopper, filter hopper below it. The filter's
	 * spare slots are full of cobblestone, as in a vanilla redstone sorter.
	 */
	private static void buildSorter(GameTestHelper helper, int filterDiamonds) {
		hopper(helper, SORTER_LINE, Direction.EAST);
		hopper(helper, SORTER_NEXT, Direction.EAST);
		hopper(helper, SORTER_FILTER, Direction.DOWN);
		put(helper, SORTER_FILTER, 0, Items.DIAMOND, filterDiamonds);
		for (int slot = 1; slot < 5; slot++) {
			put(helper, SORTER_FILTER, slot, Items.COBBLESTONE, 64);
		}
	}

	private static final BlockPos SORTER_LINE = new BlockPos(1, 3, 1);
	private static final BlockPos SORTER_NEXT = SORTER_LINE.east();
	private static final BlockPos SORTER_FILTER = SORTER_LINE.below();

	/** Both outputs are hoppers holding diamonds: the filter below wins. */
	@GameTest(maxTicks = 400)
	public void sorterFilterGetsItsItem(GameTestHelper helper) {
		buildSorter(helper, 1);
		put(helper, SORTER_NEXT, 0, Items.DIAMOND, 1);
		put(helper, SORTER_LINE, 0, Items.DIAMOND, 5);
		put(helper, SORTER_LINE, 1, Items.STICK, 3);
		helper.runAfterDelay(150, () -> {
			expectCount(helper, SORTER_FILTER, Items.DIAMOND, 6, "diamonds into the filter");
			expectCount(helper, SORTER_NEXT, Items.DIAMOND, 1, "no diamonds along the line");
			expectCount(helper, SORTER_NEXT, Items.STICK, 3, "sticks along the line");
			helper.succeed();
		});
	}

	/** A full filter hopper does not make items wait: they carry on along the line. */
	@GameTest(maxTicks = 400)
	public void fullSorterFilterDoesNotJamTheLine(GameTestHelper helper) {
		buildSorter(helper, 64);
		put(helper, SORTER_LINE, 0, Items.DIAMOND, 3);
		helper.runAfterDelay(150, () -> {
			expectCount(helper, SORTER_NEXT, Items.DIAMOND, 3, "diamonds carried on");
			helper.succeed();
		});
	}
}

package smarterhoppers.test;

import static smarterhoppers.test.TestSetup.count;
import static smarterhoppers.test.TestSetup.expectCount;
import static smarterhoppers.test.TestSetup.hopper;
import static smarterhoppers.test.TestSetup.mark;
import static smarterhoppers.test.TestSetup.put;
import static smarterhoppers.test.TowerRoutingTest.fill;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import org.slf4j.LoggerFactory;

import smarterhoppers.rules.MarkedContainerRules;
import smarterhoppers.tower.TowerCapacity;
import smarterhoppers.tower.TowerRouting;

/**
 * Phase 6, R4: the whole system, as built in the user's world.
 * <pre>
 *   y6  LINE(east) -> NEXT(down)
 *   y5  FILTER (marked barrel)   OVERFLOW
 *   y4  H0(south) -> T1(east) -> C4
 *   y3               T2(east) -> C3
 *   y2               T3(east) -> C2
 *   y1               T4(east) -> C1
 * </pre>
 * The tower column (T1..T4, C1..C4) is one block south of the filter.
 */
public class ReservationTest {
	private static final BlockPos LINE = new BlockPos(1, 6, 1);
	private static final BlockPos NEXT = LINE.east();
	private static final BlockPos FILTER = LINE.below();
	private static final BlockPos OVERFLOW = NEXT.below();
	private static final BlockPos H0 = FILTER.below();
	private static final BlockPos T1 = H0.south();
	private static final BlockPos[] HOPPERS = {H0, T1, T1.below(), T1.below(2), T1.below(3)};
	private static final BlockPos C4 = T1.east();
	private static final BlockPos C3 = C4.below();
	private static final BlockPos C2 = C3.below();
	private static final BlockPos C1 = C2.below();
	private static final int SETTLE = 520;

	private static void build(GameTestHelper helper, Item... filterItems) {
		for (BlockPos chest : new BlockPos[] {C1, C2, C3, C4}) {
			helper.setBlock(chest, Blocks.BARREL);
		}
		helper.setBlock(OVERFLOW, Blocks.CHEST);
		helper.setBlock(FILTER, Blocks.BARREL);
		mark(helper, FILTER, Direction.NORTH);
		for (int i = 0; i < filterItems.length; i++) {
			put(helper, FILTER, i, filterItems[i], 2);
		}
		hopper(helper, LINE, Direction.EAST);
		hopper(helper, NEXT, Direction.DOWN);
		hopper(helper, H0, Direction.SOUTH);
		for (BlockPos tower = T1; tower.getY() >= C1.getY(); tower = tower.below()) {
			hopper(helper, tower, Direction.EAST);
		}
	}

	private static void expectHoppersHoldNo(GameTestHelper helper, Item... items) {
		for (BlockPos hopper : HOPPERS) {
			for (Item item : items) {
				expectCount(helper, hopper, item, 0, "nothing stuck in hopper " + hopper);
			}
		}
	}

	/** The original problem 2: 10 logs of room left, 30 logs arrive, then stairs. */
	@GameTest(maxTicks = 700)
	public void onlyWhatFitsEntersTheTower(GameTestHelper helper) {
		build(helper, Items.OAK_LOG, Items.OAK_STAIRS);
		fill(helper, C1, new ItemStack(Items.OAK_LOG, 64));
		fill(helper, C2, new ItemStack(Items.OAK_LOG, 64));
		put(helper, C2, 0, Items.OAK_LOG, 54);
		put(helper, C3, 0, Items.OAK_STAIRS, 1);
		int logsBefore = count(helper, C1, Items.OAK_LOG) + count(helper, C2, Items.OAK_LOG);
		put(helper, LINE, 0, Items.OAK_LOG, 30);
		put(helper, LINE, 1, Items.OAK_STAIRS, 3);
		helper.runAfterDelay(SETTLE, () -> {
			int logsStored = count(helper, C1, Items.OAK_LOG) + count(helper, C2, Items.OAK_LOG) - logsBefore;
			helper.assertTrue(logsStored == 10, Component.literal("logs stored: " + logsStored + ", expected 10"));
			expectCount(helper, OVERFLOW, Items.OAK_LOG, 20, "logs to overflow");
			expectCount(helper, C3, Items.OAK_STAIRS, 4, "stairs reach their chest");
			expectCount(helper, FILTER, Items.OAK_LOG, 2, "filter keeps its logs");
			expectCount(helper, FILTER, Items.OAK_STAIRS, 2, "filter keeps its stairs");
			expectHoppersHoldNo(helper, Items.OAK_LOG, Items.OAK_STAIRS);
			helper.succeed();
		});
	}

	/** One empty slot shared by two items: only one slot's worth may enter. */
	@GameTest(maxTicks = 700)
	public void emptySlotsAreSharedBetweenItems(GameTestHelper helper) {
		build(helper, Items.ENDER_PEARL, Items.SNOWBALL);
		fill(helper, C4, new ItemStack(Items.COBBLESTONE, 64));
		put(helper, C4, 0, Items.ENDER_PEARL, 16);
		put(helper, C4, 1, Items.SNOWBALL, 16);
		container(helper).setItem(26, ItemStack.EMPTY);
		put(helper, LINE, 0, Items.ENDER_PEARL, 16);
		put(helper, LINE, 1, Items.SNOWBALL, 16);
		helper.runAfterDelay(SETTLE, () -> {
			int stored = count(helper, C4, Items.ENDER_PEARL) + count(helper, C4, Items.SNOWBALL) - 32;
			int overflow = count(helper, OVERFLOW, Items.ENDER_PEARL) + count(helper, OVERFLOW, Items.SNOWBALL);
			helper.assertTrue(stored == 16 && overflow == 16,
					Component.literal("stored " + stored + " (expected 16), overflow " + overflow + " (expected 16)"));
			expectHoppersHoldNo(helper, Items.ENDER_PEARL, Items.SNOWBALL);
			helper.succeed();
		});
	}

	/** Items no tower chest holds yet use any free slot, so a new tower of empty chests still fills. */
	@GameTest(maxTicks = 700)
	public void homelessItemsUseFreeSlots(GameTestHelper helper) {
		build(helper, Items.DIAMOND);
		put(helper, LINE, 0, Items.DIAMOND, 3);
		helper.runAfterDelay(200, () -> {
			int stored = 0;
			for (BlockPos chest : new BlockPos[] {C1, C2, C3, C4}) {
				stored += count(helper, chest, Items.DIAMOND);
			}
			helper.assertTrue(stored == 3, Component.literal("diamonds stored: " + stored));
			expectCount(helper, OVERFLOW, Items.DIAMOND, 0, "no diamonds to overflow");
			helper.succeed();
		});
	}

	/**
	 * Cost of the two most expensive decisions on the full layout, with realistic contents: the R4
	 * simulation (memo cleared every call) and the two-output routing of T1. Logged; the bound is
	 * only a sanity check.
	 */
	@GameTest(maxTicks = 100)
	public void decisionCost(GameTestHelper helper) {
		build(helper, Items.OAK_LOG, Items.OAK_STAIRS, Items.OAK_FENCE, Items.OAK_SLAB);
		fill(helper, C1, new ItemStack(Items.OAK_LOG, 64));
		put(helper, C2, 0, Items.OAK_LOG, 30);
		put(helper, C3, 0, Items.OAK_STAIRS, 12);
		put(helper, C4, 0, Items.OAK_FENCE, 5);
		put(helper, C4, 1, Items.OAK_SLAB, 40);
		for (BlockPos hopper : HOPPERS) {
			put(helper, hopper, 0, Items.OAK_LOG, 2);
			put(helper, hopper, 1, Items.OAK_STAIRS, 1);
		}
		helper.runAfterDelay(1, () -> {
			ServerLevel level = helper.getLevel();
			BlockPos filterPos = helper.absolutePos(FILTER);
			BlockEntity filter = level.getBlockEntity(filterPos);
			HopperBlockEntity t1 = (HopperBlockEntity) level.getBlockEntity(helper.absolutePos(T1));
			HopperBlockEntity t2 = (HopperBlockEntity) level.getBlockEntity(helper.absolutePos(T1.below()));
			ItemStack log = new ItemStack(Items.OAK_LOG);
			int runs = 5000;
			for (int i = 0; i < 500; i++) { // warm-up
				TowerCapacity.forget(level, filterPos);
				MarkedContainerRules.mayInsert(filter, log);
				TowerRouting.mayPassDown(t1, t2, log);
			}
			long start = System.nanoTime();
			for (int i = 0; i < runs; i++) {
				TowerCapacity.forget(level, filterPos);
				MarkedContainerRules.mayInsert(filter, log);
			}
			double r4Micros = (System.nanoTime() - start) / 1000.0 / runs;
			start = System.nanoTime();
			for (int i = 0; i < runs; i++) {
				TowerRouting.mayPassDown(t1, t2, log);
			}
			double routeMicros = (System.nanoTime() - start) / 1000.0 / runs;
			LoggerFactory.getLogger("smarter_hoppers_test").info(
					"Decision cost: R4 simulation {} µs, tower routing {} µs",
					String.format("%.2f", r4Micros), String.format("%.2f", routeMicros));
			helper.assertTrue(r4Micros < 500 && routeMicros < 500,
					Component.literal("too slow: R4 " + r4Micros + " µs, routing " + routeMicros + " µs"));
			helper.succeed();
		});
	}

	private static Container container(GameTestHelper helper) {
		return TestSetup.container(helper, C4);
	}
}

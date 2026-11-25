package smarterhoppers.test;

import static smarterhoppers.test.TestSetup.container;
import static smarterhoppers.test.TestSetup.expectCount;
import static smarterhoppers.test.TestSetup.hopper;
import static smarterhoppers.test.TestSetup.mark;
import static smarterhoppers.test.TestSetup.put;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Phase 4, R3. Layout (side view, line running east):
 * <pre>
 *   LINE(east) -> NEXT(down)
 *   FILTER        OVERFLOW
 * </pre>
 * FILTER is a barrel below the first line hopper; OVERFLOW stands for the rest of the system.
 */
public class LineRoutingTest {
	private static final BlockPos LINE = new BlockPos(1, 3, 1);
	private static final BlockPos NEXT = LINE.east();
	private static final BlockPos FILTER = LINE.below();
	private static final BlockPos OVERFLOW = NEXT.below();
	private static final int SETTLE = 100;

	private static void build(GameTestHelper helper, boolean marked) {
		hopper(helper, LINE, Direction.EAST);
		hopper(helper, NEXT, Direction.DOWN);
		helper.setBlock(FILTER, Blocks.BARREL);
		helper.setBlock(OVERFLOW, Blocks.CHEST);
		if (marked) {
			mark(helper, FILTER, Direction.NORTH);
		}
		put(helper, FILTER, 0, Items.DIAMOND, 2);
	}

	@GameTest(maxTicks = 300)
	public void lineFeedsMarkedContainerBelow(GameTestHelper helper) {
		build(helper, true);
		put(helper, LINE, 0, Items.EMERALD, 2);
		put(helper, LINE, 1, Items.DIAMOND, 3);
		put(helper, LINE, 2, Items.GOLD_INGOT, 1);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, FILTER, Items.DIAMOND, 5, "diamonds into the filter");
			expectCount(helper, FILTER, Items.EMERALD, 0, "no emeralds in the filter");
			expectCount(helper, OVERFLOW, Items.DIAMOND, 0, "no diamonds to overflow");
			expectCount(helper, OVERFLOW, Items.EMERALD, 2, "emeralds to overflow");
			expectCount(helper, OVERFLOW, Items.GOLD_INGOT, 1, "gold to overflow");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 300)
	public void fullFilterLetsItemsPass(GameTestHelper helper) {
		build(helper, true);
		Container filter = container(helper, FILTER);
		for (int slot = 0; slot < filter.getContainerSize(); slot++) {
			filter.setItem(slot, new ItemStack(Items.DIAMOND, 64));
		}
		put(helper, LINE, 0, Items.DIAMOND, 3);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, OVERFLOW, Items.DIAMOND, 3, "diamonds passed the full filter");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 300)
	public void unmarkedContainerBelowIsIgnored(GameTestHelper helper) {
		build(helper, false);
		put(helper, LINE, 0, Items.DIAMOND, 3);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, FILTER, Items.DIAMOND, 2, "unmarked barrel untouched");
			expectCount(helper, OVERFLOW, Items.DIAMOND, 3, "everything carried on");
			helper.succeed();
		});
	}
}

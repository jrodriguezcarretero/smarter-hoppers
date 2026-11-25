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
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Phase 3: R1 (filter items stay), R2 (only items already inside go in) and locked marker frames. */
public class MarkedContainerRulesTest {
	private static final BlockPos BARREL = new BlockPos(1, 2, 1);
	private static final BlockPos BELOW = BARREL.below();
	private static final BlockPos ABOVE = BARREL.above();
	/** Long enough for a hopper to move a handful of items (8 ticks each). */
	private static final int SETTLE = 80;

	@GameTest(maxTicks = 200)
	public void r1KeepsTwoOfEachItem(GameTestHelper helper) {
		helper.setBlock(BARREL, Blocks.BARREL);
		mark(helper, BARREL, Direction.EAST);
		put(helper, BARREL, 0, Items.DIAMOND, 3);
		put(helper, BARREL, 1, Items.EMERALD, 2);
		// Split across slots: 1 + 1 + 1 gold counts as 3.
		put(helper, BARREL, 2, Items.GOLD_INGOT, 1);
		put(helper, BARREL, 3, Items.GOLD_INGOT, 1);
		put(helper, BARREL, 4, Items.GOLD_INGOT, 1);
		hopper(helper, BELOW, Direction.DOWN);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, BARREL, Items.DIAMOND, 2, "diamonds kept");
			expectCount(helper, BARREL, Items.EMERALD, 2, "emeralds kept");
			expectCount(helper, BARREL, Items.GOLD_INGOT, 2, "gold kept");
			expectCount(helper, BELOW, Items.DIAMOND, 1, "diamonds pulled");
			expectCount(helper, BELOW, Items.GOLD_INGOT, 1, "gold pulled");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void r1DoesNotApplyToUnmarkedContainers(GameTestHelper helper) {
		helper.setBlock(BARREL, Blocks.BARREL);
		put(helper, BARREL, 0, Items.DIAMOND, 3);
		hopper(helper, BELOW, Direction.DOWN);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, BARREL, Items.DIAMOND, 0, "unmarked barrel emptied");
			expectCount(helper, BELOW, Items.DIAMOND, 3, "all pulled");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void r2OnlyAcceptsItemsAlreadyInside(GameTestHelper helper) {
		helper.setBlock(BARREL, Blocks.BARREL);
		mark(helper, BARREL, Direction.EAST);
		put(helper, BARREL, 0, Items.DIAMOND, 2);
		hopper(helper, ABOVE, Direction.DOWN);
		put(helper, ABOVE, 0, Items.EMERALD, 1);
		put(helper, ABOVE, 1, Items.DIAMOND, 3);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, BARREL, Items.DIAMOND, 5, "diamonds accepted");
			expectCount(helper, BARREL, Items.EMERALD, 0, "emerald rejected");
			expectCount(helper, ABOVE, Items.EMERALD, 1, "emerald still in hopper");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void r2DoesNotApplyToUnmarkedContainers(GameTestHelper helper) {
		helper.setBlock(BARREL, Blocks.BARREL);
		put(helper, BARREL, 0, Items.DIAMOND, 2);
		hopper(helper, ABOVE, Direction.DOWN);
		put(helper, ABOVE, 0, Items.EMERALD, 1);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, BARREL, Items.EMERALD, 1, "emerald accepted");
			helper.succeed();
		});
	}

	/** Double chest, left half full of diamonds: diamonds must still go into the right half. */
	@GameTest(maxTicks = 200)
	public void r2CountsBothHalvesOfADoubleChest(GameTestHelper helper) {
		BlockPos left = new BlockPos(1, 2, 1);
		BlockPos right = left.east();
		placeDoubleChest(helper, left, right);
		mark(helper, right, Direction.SOUTH);
		Container leftHalf = container(helper, left);
		for (int slot = 0; slot < leftHalf.getContainerSize(); slot++) {
			leftHalf.setItem(slot, new ItemStack(Items.DIAMOND, 64));
		}
		hopper(helper, right.above(), Direction.DOWN);
		put(helper, right.above(), 0, Items.DIAMOND, 3);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, right, Items.DIAMOND, 3, "diamonds in the right half");
			helper.succeed();
		});
	}

	/** Double chest, all diamonds in the right half, hopper under the left half. */
	@GameTest(maxTicks = 200)
	public void r1CountsBothHalvesOfADoubleChest(GameTestHelper helper) {
		BlockPos left = new BlockPos(1, 2, 1);
		BlockPos right = left.east();
		placeDoubleChest(helper, left, right);
		mark(helper, right, Direction.SOUTH);
		put(helper, right, 0, Items.DIAMOND, 3);
		hopper(helper, left.below(), Direction.DOWN);
		helper.runAfterDelay(SETTLE, () -> {
			expectCount(helper, right, Items.DIAMOND, 2, "diamonds kept");
			expectCount(helper, left.below(), Items.DIAMOND, 1, "diamond pulled");
			helper.succeed();
		});
	}

	/** Fast Item Frames: a hopper under a marker frame block must not pull the comparator out. */
	@GameTest(maxTicks = 200)
	public void markerFrameBlockKeepsItsComparator(GameTestHelper helper) {
		Block frameBlock = TestSetup.fastItemFramesBlock();
		if (frameBlock == null) {
			helper.succeed(); // Fast Item Frames not installed
			return;
		}
		helper.setBlock(BARREL, Blocks.BARREL);
		BlockPos framePos = BARREL.east();
		Container frame = TestSetup.frameBlock(helper, frameBlock, framePos, Direction.EAST, new ItemStack(Items.COMPARATOR));
		hopper(helper, framePos.below(), Direction.DOWN);
		// A hopper facing into the frame must not push into it either.
		hopper(helper, framePos.east(), Direction.WEST);
		put(helper, framePos.east(), 0, Items.DIAMOND, 1);
		helper.runAfterDelay(SETTLE, () -> {
			helper.assertTrue(frame.getItem(0).is(Items.COMPARATOR) && frame.getItem(0).getCount() == 1,
					Component.literal("frame should still hold exactly one comparator, has " + frame.getItem(0)));
			expectCount(helper, framePos.below(), Items.COMPARATOR, 0, "comparator not pulled");
			expectCount(helper, framePos.east(), Items.DIAMOND, 1, "diamond not pushed into the frame");
			helper.succeed();
		});
	}

	private static void placeDoubleChest(GameTestHelper helper, BlockPos left, BlockPos right) {
		helper.setBlock(left, Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
		helper.setBlock(right, Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
	}
}

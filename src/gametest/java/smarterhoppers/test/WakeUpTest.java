package smarterhoppers.test;

import static smarterhoppers.test.TestSetup.container;
import static smarterhoppers.test.TestSetup.count;
import static smarterhoppers.test.TestSetup.expectCount;
import static smarterhoppers.test.TestSetup.hopper;
import static smarterhoppers.test.TestSetup.mark;
import static smarterhoppers.test.TestSetup.put;
import static smarterhoppers.test.TowerRoutingTest.C1;
import static smarterhoppers.test.TowerRoutingTest.T1;
import static smarterhoppers.test.TowerRoutingTest.buildWoodTower;
import static smarterhoppers.test.TowerRoutingTest.fill;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Phase 7: hoppers must resume when a decision changes for reasons Lithium does not track
 * (room freed in a chest further down, a marker added), even after Lithium put them to sleep.
 */
public class WakeUpTest {
	/** Logs wait in T1 because their chests are full; freeing room in C1 lets them through. */
	@GameTest(maxTicks = 500)
	public void waitingItemsMoveWhenRoomFrees(GameTestHelper helper) {
		buildWoodTower(helper);
		fill(helper, TowerRoutingTest.C1, new ItemStack(Items.OAK_LOG, 64));
		fill(helper, TowerRoutingTest.C2, new ItemStack(Items.OAK_LOG, 64));
		put(helper, T1, 0, Items.OAK_LOG, 3);
		// Long enough for every hopper to give up and fall asleep.
		helper.runAfterDelay(100, () -> {
			expectCount(helper, T1, Items.OAK_LOG, 3, "logs waiting in T1");
			container(helper, C1).setItem(5, ItemStack.EMPTY);
			container(helper, C1).setChanged();
			helper.runAfterDelay(150, () -> {
				expectCount(helper, T1, Items.OAK_LOG, 0, "logs left T1");
				helper.assertTrue(count(helper, C1, Items.OAK_LOG) == 26 * 64 + 3,
						Component.literal("logs reached C1: " + count(helper, C1, Items.OAK_LOG)));
				helper.succeed();
			});
		});
	}

	/** A line hopper stuck behind a full next hopper starts feeding the container below once it is marked. */
	@GameTest(maxTicks = 400)
	public void lineResumesWhenContainerBelowGetsMarked(GameTestHelper helper) {
		BlockPos line = new BlockPos(1, 3, 1);
		BlockPos next = line.east();
		BlockPos filter = line.below();
		hopper(helper, line, Direction.EAST);
		hopper(helper, next, Direction.EAST);
		for (int slot = 0; slot < 5; slot++) {
			put(helper, next, slot, Items.COBBLESTONE, 64);
		}
		helper.setBlock(filter, Blocks.BARREL);
		put(helper, filter, 0, Items.DIAMOND, 2);
		put(helper, line, 0, Items.DIAMOND, 3);
		helper.runAfterDelay(100, () -> {
			expectCount(helper, line, Items.DIAMOND, 3, "diamonds stuck before marking");
			ItemFrame frame = mark(helper, filter, Direction.NORTH);
			helper.runAfterDelay(100, () -> {
				helper.assertTrue(frame.isAlive(), Component.literal("frame gone"));
				expectCount(helper, filter, Items.DIAMOND, 5, "diamonds into the newly marked filter");
				helper.succeed();
			});
		});
	}
}

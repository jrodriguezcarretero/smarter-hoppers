package smarterhoppers.test;

import static smarterhoppers.test.TestSetup.mark;
import static smarterhoppers.test.TestSetup.put;
import static smarterhoppers.test.TowerRoutingTest.T1;
import static smarterhoppers.test.TowerRoutingTest.buildWoodTower;
import static smarterhoppers.test.TowerRoutingTest.fill;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import smarterhoppers.diagnose.Diagnosis;

/** Phase 8: the diagnosis behind /smarterhoppers diagnose. */
public class DiagnosisTest {
	@GameTest(maxTicks = 200)
	public void reportsWaitingItemsAndMarkers(GameTestHelper helper) {
		buildWoodTower(helper);
		fill(helper, TowerRoutingTest.C1, new ItemStack(Items.OAK_LOG, 64));
		fill(helper, TowerRoutingTest.C2, new ItemStack(Items.OAK_LOG, 64));
		put(helper, T1, 0, Items.OAK_LOG, 3);
		BlockPos filter = T1.above();
		helper.setBlock(filter, Blocks.BARREL);
		mark(helper, filter, Direction.WEST);
		helper.runAfterDelay(40, () -> {
			Diagnosis diagnosis = Diagnosis.scan(helper.getLevel(), helper.absolutePos(T1), 4);
			helper.assertTrue(diagnosis.markedContainers() == 1 && diagnosis.markedFeedingTowers() == 1,
					Component.literal("marked: " + diagnosis));
			helper.assertTrue(diagnosis.twoOutputHoppers() == 3, Component.literal("two-output hoppers: " + diagnosis));
			helper.assertTrue(diagnosis.waiting().size() == 1
					&& diagnosis.waiting().getFirst().hopper().equals(helper.absolutePos(T1))
					&& diagnosis.waiting().getFirst().item().is(Items.OAK_LOG),
					Component.literal("waiting: " + diagnosis.waiting()));

			// The command itself parses and runs; it returns the number of waiting items.
			MinecraftServer server = helper.getLevel().getServer();
			CommandSourceStack source = server.createCommandSourceStack()
					.withLevel(helper.getLevel())
					.withPosition(Vec3.atCenterOf(helper.absolutePos(T1)))
					.withSuppressedOutput();
			try {
				int result = server.getCommands().getDispatcher().execute("smarterhoppers diagnose 4", source);
				helper.assertTrue(result == 1, Component.literal("command returned " + result));
			} catch (CommandSyntaxException e) {
				helper.fail(Component.literal("command failed: " + e.getMessage()));
			}
			helper.succeed();
		});
	}
}

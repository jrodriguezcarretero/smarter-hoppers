package smarterhoppers.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

import smarterhoppers.marker.Markers;

/** Phase 2: detection of marked containers (item frame holding a comparator). */
public class MarkerTest {
	private static final BlockPos BARREL = new BlockPos(1, 1, 1);
	/** East of the barrel; a frame there facing east hangs on the barrel. */
	private static final BlockPos FRAME = BARREL.east();

	private static boolean marked(GameTestHelper helper, BlockPos relative) {
		return Markers.isMarked(helper.getLevel(), helper.absolutePos(relative));
	}

	private static void expect(GameTestHelper helper, BlockPos relative, boolean expected, String what) {
		helper.assertTrue(marked(helper, relative) == expected,
				Component.literal(what + ": expected marked=" + expected));
	}

	/**
	 * Spawns a vanilla item frame entity. A light block keeps Fast Item Frames
	 * (if present) from turning it into a frame block.
	 */
	private static ItemFrame spawnFrameEntity(GameTestHelper helper, BlockPos relative, Direction facing, ItemStack item) {
		helper.setBlock(relative, Blocks.LIGHT);
		ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(relative), facing);
		frame.setItem(item, false);
		helper.getLevel().addFreshEntity(frame);
		return frame;
	}

	@GameTest
	public void vanillaFrameMarksBarrel(GameTestHelper helper) {
		helper.setBlock(BARREL, Blocks.BARREL);
		expect(helper, BARREL, false, "bare barrel");

		ItemFrame frame = spawnFrameEntity(helper, FRAME, Direction.EAST, new ItemStack(Items.COMPARATOR));
		// Give Fast Item Frames its conversion tick, then check the frame is still an entity.
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(frame.isAlive(), Component.literal("frame entity was removed"));
			expect(helper, BARREL, true, "frame with comparator");

			frame.setItem(ItemStack.EMPTY);
			expect(helper, BARREL, false, "empty frame");

			frame.setItem(new ItemStack(Items.DIAMOND));
			expect(helper, BARREL, false, "frame with diamond");

			frame.setItem(new ItemStack(Items.COMPARATOR));
			expect(helper, BARREL, true, "comparator put back");

			frame.discard();
			helper.runAfterDelay(1, () -> {
				expect(helper, BARREL, false, "frame removed");
				helper.succeed();
			});
		});
	}

	@GameTest
	public void frameOnOtherBlockDoesNotMark(GameTestHelper helper) {
		helper.setBlock(BARREL, Blocks.BARREL);
		helper.setBlock(BARREL.above(), Blocks.STONE);
		// Hangs on the stone above the barrel, not on the barrel.
		spawnFrameEntity(helper, BARREL.above().east(), Direction.EAST, new ItemStack(Items.COMPARATOR));
		helper.runAfterDelay(2, () -> {
			expect(helper, BARREL, false, "frame on neighbouring stone");
			expect(helper, BARREL.above(), true, "the stone itself");
			helper.succeed();
		});
	}

	@GameTest
	public void doubleChestMarkedFromEitherHalf(GameTestHelper helper) {
		BlockPos left = new BlockPos(1, 1, 1);
		BlockPos right = left.east();
		helper.setBlock(left, Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
		helper.setBlock(right, Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
		spawnFrameEntity(helper, right.south(), Direction.SOUTH, new ItemStack(Items.COMPARATOR));
		helper.runAfterDelay(2, () -> {
			expect(helper, right, true, "half with the frame");
			expect(helper, left, true, "other half");
			helper.succeed();
		});
	}

	@GameTest
	public void fastItemFramesBlockMarksBarrel(GameTestHelper helper) {
		Block frameBlock = BuiltInRegistries.BLOCK
				.getOptional(Identifier.fromNamespaceAndPath("fastitemframes", "item_frame")).orElse(null);
		if (frameBlock == null) {
			helper.succeed(); // Fast Item Frames not installed
			return;
		}
		helper.setBlock(BARREL, Blocks.BARREL);
		helper.setBlock(FRAME, frameBlock.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
		Container frame = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(FRAME));
		helper.assertTrue(frame != null, Component.literal("frame block has no block entity"));
		expect(helper, BARREL, false, "empty frame block");

		frame.setItem(0, new ItemStack(Items.COMPARATOR));
		expect(helper, BARREL, true, "frame block with comparator");
		helper.assertTrue(Markers.isMarkerFrameContainer(frame), Component.literal("frame block should be a marker frame"));

		frame.setItem(0, new ItemStack(Items.DIAMOND));
		expect(helper, BARREL, false, "frame block with diamond");
		helper.assertTrue(!Markers.isMarkerFrameContainer(frame), Component.literal("frame block holds no marker"));

		frame.setItem(0, new ItemStack(Items.COMPARATOR));
		helper.setBlock(FRAME, Blocks.AIR);
		expect(helper, BARREL, false, "frame block removed");
		helper.succeed();
	}
}

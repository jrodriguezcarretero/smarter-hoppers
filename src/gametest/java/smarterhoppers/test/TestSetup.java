package smarterhoppers.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.jspecify.annotations.Nullable;

/** Shared helpers for building test setups. */
final class TestSetup {
	private TestSetup() {
	}

	/**
	 * Spawns a vanilla item frame entity. A light block keeps Fast Item Frames
	 * (if present) from turning it into a frame block.
	 */
	static ItemFrame frameEntity(GameTestHelper helper, BlockPos relative, Direction facing, ItemStack item) {
		helper.setBlock(relative, Blocks.LIGHT);
		ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(relative), facing);
		frame.setItem(item, false);
		helper.getLevel().addFreshEntity(frame);
		return frame;
	}

	/** Marks the container at {@code container} with a comparator frame on its {@code side} face. */
	static ItemFrame mark(GameTestHelper helper, BlockPos container, Direction side) {
		return frameEntity(helper, container.relative(side), side, new ItemStack(Items.COMPARATOR));
	}

	static @Nullable Block fastItemFramesBlock() {
		return BuiltInRegistries.BLOCK
				.getOptional(Identifier.fromNamespaceAndPath("fastitemframes", "item_frame")).orElse(null);
	}

	/** Places a Fast Item Frames frame block hanging on the block opposite to {@code facing}. */
	static Container frameBlock(GameTestHelper helper, Block frameBlock, BlockPos relative, Direction facing, ItemStack item) {
		helper.setBlock(relative, frameBlock.defaultBlockState().setValue(BlockStateProperties.FACING, facing));
		Container frame = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
		helper.assertTrue(frame != null, Component.literal("frame block has no block entity"));
		frame.setItem(0, item);
		return frame;
	}

	static void hopper(GameTestHelper helper, BlockPos relative, Direction facing) {
		helper.setBlock(relative, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, facing));
	}

	static Container container(GameTestHelper helper, BlockPos relative) {
		Container container = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
		helper.assertTrue(container != null, Component.literal("no container at " + relative));
		return container;
	}

	static void put(GameTestHelper helper, BlockPos relative, int slot, Item item, int count) {
		container(helper, relative).setItem(slot, new ItemStack(item, count));
	}

	static int count(GameTestHelper helper, BlockPos relative, Item item) {
		return container(helper, relative).countItem(item);
	}

	static void expectCount(GameTestHelper helper, BlockPos relative, Item item, int expected, String what) {
		int actual = count(helper, relative, item);
		helper.assertTrue(actual == expected,
				Component.literal(what + ": expected " + expected + " " + item + ", found " + actual));
	}
}

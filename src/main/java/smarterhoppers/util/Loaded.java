package smarterhoppers.util;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block lookups that never load chunks. Smarter Hoppers looks further than vanilla hoppers (down a
 * tower, up a line), and on the server a plain lookup in an unloaded chunk loads it synchronously,
 * which stalls the game and can hang the server while it shuts down.
 */
public final class Loaded {
	private Loaded() {
	}

	public static @Nullable BlockEntity blockEntity(Level level, BlockPos pos) {
		return level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
	}

	public static BlockState blockState(Level level, BlockPos pos) {
		return level.isLoaded(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
	}
}

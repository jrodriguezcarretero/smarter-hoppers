package smarterhoppers.util;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Block lookups that never load or wait for chunks. Smarter Hoppers looks further than vanilla
 * hoppers (down a tower, up a line), and on the server a plain lookup in a chunk that is not fully
 * loaded blocks until it is. That stalls the game, hangs the server while it shuts down, and
 * deadlocks it while it loads chunks (a lookup from a chunk's load event waiting for a neighbour
 * that is still loading). A chunk that is not fully loaded reads as air.
 */
public final class Loaded {
	private Loaded() {
	}

	public static boolean isLoaded(Level level, BlockPos pos) {
		return chunk(level, pos) != null;
	}

	public static @Nullable BlockEntity blockEntity(Level level, BlockPos pos) {
		LevelChunk chunk = chunk(level, pos);
		return chunk == null ? null : chunk.getBlockEntity(pos);
	}

	public static BlockState blockState(Level level, BlockPos pos) {
		LevelChunk chunk = chunk(level, pos);
		return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
	}

	private static @Nullable LevelChunk chunk(Level level, BlockPos pos) {
		if (level.isOutsideBuildHeight(pos)) {
			return null;
		}
		return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
	}
}

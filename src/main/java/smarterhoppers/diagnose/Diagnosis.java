package smarterhoppers.diagnose;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import smarterhoppers.marker.Markers;
import smarterhoppers.tower.TowerCapacity;
import smarterhoppers.tower.TowerRouting;
import smarterhoppers.tower.Towers;

/** What Smarter Hoppers sees around a position: marked containers, two-output hoppers, waiting items. */
public record Diagnosis(int markedContainers, int markedFeedingTowers, int twoOutputHoppers, List<Waiting> waiting) {
	/** An item held in a hopper because the chest that holds it is full. */
	public record Waiting(BlockPos hopper, ItemStack item) {
	}

	public static Diagnosis scan(ServerLevel level, BlockPos center, int radius) {
		int marked = 0;
		int feedingTowers = 0;
		int twoOutputs = 0;
		List<Waiting> waiting = new ArrayList<>();
		int minChunkX = (center.getX() - radius) >> 4;
		int maxChunkX = (center.getX() + radius) >> 4;
		int minChunkZ = (center.getZ() - radius) >> 4;
		int maxChunkZ = (center.getZ() + radius) >> 4;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (!level.hasChunk(chunkX, chunkZ)) {
					continue;
				}
				LevelChunk chunk = level.getChunk(chunkX, chunkZ);
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					BlockPos pos = blockEntity.getBlockPos();
					if (pos.distChessboard(center) > radius) {
						continue;
					}
					if (blockEntity instanceof HopperBlockEntity hopper) {
						if (Towers.twoOutputTarget(level, pos, hopper.getBlockState()) == null) {
							continue;
						}
						twoOutputs++;
						for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
							ItemStack item = hopper.getItem(slot);
							if (!item.isEmpty() && TowerRouting.isWaiting(level, pos, hopper, item)) {
								waiting.add(new Waiting(pos, item.copy()));
							}
						}
					} else if (blockEntity instanceof Container && Markers.isMarked(level, pos)) {
						marked++;
						if (TowerCapacity.feedsTower(level, pos)) {
							feedingTowers++;
						}
					}
				}
			}
		}
		return new Diagnosis(marked, feedingTowers, twoOutputs, waiting);
	}
}

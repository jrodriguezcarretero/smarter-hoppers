package smarterhoppers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smarterhoppers.marker.FastItemFramesCompat;
import smarterhoppers.marker.Markers;

public class SmarterHoppers implements ModInitializer {
	public static final String MOD_ID = "smarter_hoppers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Keep the marked-container cache in sync with item frames appearing and disappearing.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemFrame frame) {
				Markers.invalidate(level, Markers.supportOf(frame));
			}
		});
		// A frame that is broken or removed lets its hoppers know; one unloading with its chunk only
		// drops the cache entry, since the neighbourhood may be unloading too.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof ItemFrame frame) {
				Entity.RemovalReason reason = frame.getRemovalReason();
				if (reason != null && reason.shouldDestroy()) {
					Markers.invalidate(level, Markers.supportOf(frame));
				} else {
					Markers.forget(level, Markers.supportOf(frame));
				}
			}
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
			if (FastItemFramesCompat.isFrameBlockEntity(blockEntity)) {
				Markers.invalidateAround(level, blockEntity.getBlockPos());
			}
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> {
			if (FastItemFramesCompat.isFrameBlockEntity(blockEntity)) {
				Markers.forgetAround(level, blockEntity.getBlockPos());
			}
		});
		ServerLevelEvents.UNLOAD.register((server, level) -> Markers.clear(level));

		FabricLoader loader = FabricLoader.getInstance();
		LOGGER.info("Smarter Hoppers loaded (Lithium: {}, Fast Item Frames: {})",
				loader.isModLoaded("lithium"), loader.isModLoaded("fastitemframes"));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

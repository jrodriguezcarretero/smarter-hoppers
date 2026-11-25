package smarterhoppers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmarterHoppers implements ModInitializer {
	public static final String MOD_ID = "smarter_hoppers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		FabricLoader loader = FabricLoader.getInstance();
		LOGGER.info("Smarter Hoppers loaded (Lithium: {}, Fast Item Frames: {})",
				loader.isModLoaded("lithium"), loader.isModLoaded("fastitemframes"));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

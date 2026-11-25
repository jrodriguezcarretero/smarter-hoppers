package smarterhoppers.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import smarterhoppers.SmarterHoppers;

/** Phase 1: the mod loads (with or without Lithium, see -PnoLithium). */
public class LoadTest {
	@GameTest
	public void loads(GameTestHelper helper) {
		FabricLoader loader = FabricLoader.getInstance();
		helper.assertTrue(loader.isModLoaded(SmarterHoppers.MOD_ID), Component.literal("Smarter Hoppers is not loaded"));
		helper.succeed();
	}
}

package smarterhoppers.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import smarterhoppers.SmarterHoppers;

/** Phase 1: the mod loads alongside Lithium. */
public class LoadTest {
	@GameTest
	public void loadsWithLithium(GameTestHelper helper) {
		FabricLoader loader = FabricLoader.getInstance();
		helper.assertTrue(loader.isModLoaded(SmarterHoppers.MOD_ID), Component.literal("Smarter Hoppers is not loaded"));
		helper.assertTrue(loader.isModLoaded("lithium"), Component.literal("Lithium is not loaded"));
		helper.succeed();
	}
}

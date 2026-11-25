package smarterhoppers.compat;

import net.caffeinemc.mods.lithium.api.inventory.LithiumDefaultedList;
import net.caffeinemc.mods.lithium.api.inventory.LithiumInventory;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.world.Container;

/**
 * Lithium skips hopper transfers that failed before until the inventories involved change, and puts
 * idle hoppers to sleep. Smarter Hoppers' decisions also depend on other blocks (chests further down
 * a tower, markers), so it tells Lithium when they may have changed, through Lithium's public API.
 */
public final class LithiumCompat {
	public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("lithium");

	private LithiumCompat() {
	}

	/** Makes Lithium re-evaluate transfers involving {@code container} and wake hoppers waiting on it. */
	public static void retry(Container container) {
		if (LOADED) {
			Api.retry(container);
		}
	}

	/** Only loaded when Lithium is present. */
	private static final class Api {
		static void retry(Container container) {
			if (container instanceof LithiumInventory inventory
					&& inventory.getInventoryLithium() instanceof LithiumDefaultedList list) {
				list.changedInteractionConditions();
			}
		}
	}
}

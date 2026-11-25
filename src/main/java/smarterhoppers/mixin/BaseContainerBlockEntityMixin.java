package smarterhoppers.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import smarterhoppers.rules.MarkedContainerRules;
import smarterhoppers.tower.TowerRouting;

/**
 * Adds R1/R2 to every container block entity that keeps {@link Container}'s default insertion and
 * extraction checks (barrels, chests, shulker boxes, dispensers, droppers, hoppers...). Both vanilla
 * hoppers and Lithium's hopper code ask these methods, as do other mods' item transfers.
 */
@Mixin(BaseContainerBlockEntity.class)
abstract class BaseContainerBlockEntityMixin {
	/** Replaces {@code Container#canPlaceItem}'s default, which always returns true. */
	public boolean canPlaceItem(int slot, ItemStack itemStack) {
		return MarkedContainerRules.mayInsert((BaseContainerBlockEntity) (Object) this, itemStack);
	}

	/**
	 * Replaces {@code Container#canTakeItem}'s default, which always returns true. Also where tower
	 * hoppers decide what the hopper below may pull from them (T2/T3).
	 */
	public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
		Object self = this;
		return MarkedContainerRules.mayExtract((BaseContainerBlockEntity) self, itemStack)
				&& (!(self instanceof HopperBlockEntity hopper) || TowerRouting.mayPassDown(hopper, into, itemStack));
	}
}

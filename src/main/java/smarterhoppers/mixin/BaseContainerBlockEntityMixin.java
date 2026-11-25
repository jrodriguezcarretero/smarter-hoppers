package smarterhoppers.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

import smarterhoppers.rules.MarkedContainerRules;

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

	/** Replaces {@code Container#canTakeItem}'s default, which always returns true. */
	public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
		return MarkedContainerRules.mayExtract((BaseContainerBlockEntity) (Object) this, itemStack);
	}
}

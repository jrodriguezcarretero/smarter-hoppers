package smarterhoppers.tower;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import smarterhoppers.util.Loaded;

/**
 * Routing for hoppers with two outputs: the container they face (a chest, a barrel, another
 * hopper...) and the hopper right below them, which pulls from them. Each item goes to the output
 * that already holds it:
 * <ol>
 * <li>a faced container (not a hopper) that holds the item and has room for it;</li>
 * <li>otherwise the hopper below, if it holds the item and has room, or if a chest further down
 * the column does (see {@link Towers});</li>
 * <li>otherwise a faced hopper that holds the item and has room. When both outputs are hoppers
 * holding the item, the one below wins, as the filter hopper of a hopper sorter.</li>
 * </ol>
 * If no output takes it but a chest of the column holds the item (its home is full), the item waits.
 * Anything else moves as in vanilla, so towers of empty chests and full filter hoppers keep working.
 */
public final class TowerRouting {
	private TowerRouting() {
	}

	private enum Route {
		FACED, BELOW, WAIT, VANILLA
	}

	/** Replaces {@code ejectItems} for hoppers with two outputs; null for any other hopper. */
	public static @Nullable Boolean eject(Level level, BlockPos pos, HopperBlockEntity hopper) {
		BlockState state = hopper.getBlockState();
		Container faced = Towers.twoOutputTarget(level, pos, state);
		if (faced == null) {
			return null;
		}
		Direction face = state.getValue(HopperBlock.FACING).getOpposite();
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			ItemStack itemStack = hopper.getItem(slot);
			if (itemStack.isEmpty()) {
				continue;
			}
			Route route = route(level, pos, state, faced, itemStack);
			if (route != Route.FACED && route != Route.VANILLA) {
				continue;
			}
			int originalCount = itemStack.getCount();
			ItemStack rest = HopperBlockEntity.addItem(hopper, faced, hopper.removeItem(slot, 1), face);
			if (rest.isEmpty()) {
				faced.setChanged();
				return true;
			}
			// Same restore as vanilla's ejectItems.
			itemStack.setCount(originalCount);
			if (originalCount == 1) {
				hopper.setItem(slot, itemStack);
			}
		}
		return false;
	}

	/** For {@code canTakeItem} of a hopper: whether {@code into} may pull {@code stack} from it. */
	public static boolean mayPassDown(HopperBlockEntity self, Container into, ItemStack stack) {
		Level level = self.getLevel();
		BlockPos pos = self.getBlockPos();
		if (level == null || level.isClientSide()
				|| !(into instanceof HopperBlockEntity below) || !below.getBlockPos().equals(pos.below())) {
			return true;
		}
		BlockState state = self.getBlockState();
		Container faced = Towers.twoOutputTarget(level, pos, state);
		if (faced == null) {
			return true;
		}
		Route route = route(level, pos, state, faced, stack);
		return route == Route.BELOW || route == Route.VANILLA;
	}

	/** Whether {@code stack} in this two-output hopper waits because the chest that holds it is full. */
	public static boolean isWaiting(Level level, BlockPos pos, HopperBlockEntity hopper, ItemStack stack) {
		BlockState state = hopper.getBlockState();
		Container faced = Towers.twoOutputTarget(level, pos, state);
		return faced != null && route(level, pos, state, faced, stack) == Route.WAIT;
	}

	private static Route route(Level level, BlockPos pos, BlockState state, Container faced, ItemStack stack) {
		Direction face = state.getValue(HopperBlock.FACING).getOpposite();
		boolean facedIsHopper = faced instanceof HopperBlockEntity;
		boolean facedTakes = Towers.contains(faced, stack) && Towers.hasRoom(faced, stack, face);
		if (facedTakes && !facedIsHopper) {
			return Route.FACED;
		}
		if (belowTakes(level, pos.below(), stack)) {
			return Route.BELOW;
		}
		if (facedTakes) {
			return Route.FACED;
		}
		return Towers.hasHome(level, pos, stack) ? Route.WAIT : Route.VANILLA;
	}

	/** The hopper below holds the item and has room, or a chest further down the column does. */
	private static boolean belowTakes(Level level, BlockPos belowPos, ItemStack stack) {
		if (Loaded.blockEntity(level, belowPos) instanceof HopperBlockEntity below
				&& Towers.contains(below, stack) && Towers.hasRoom(below, stack, Direction.UP)) {
			return true;
		}
		return Towers.canStore(level, belowPos, stack);
	}
}

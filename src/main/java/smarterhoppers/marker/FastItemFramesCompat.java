package smarterhoppers.marker;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Soft compatibility with Fast Item Frames, which turns item frames into blocks
 * ({@code fastitemframes:item_frame} / {@code fastitemframes:glow_item_frame}).
 * The frame block's {@code FACING} points away from the block it hangs on, and the
 * framed item lives in slot 0 of its block entity, which is a plain {@link Container}.
 * Looked up by registry id so the mod does not need Fast Item Frames to compile or run.
 */
public final class FastItemFramesCompat {
	private static final Identifier BLOCK_ENTITY_ID = Identifier.fromNamespaceAndPath("fastitemframes", "item_frame");
	private static final Identifier BLOCK_ID = Identifier.fromNamespaceAndPath("fastitemframes", "item_frame");
	private static final Identifier GLOW_BLOCK_ID = Identifier.fromNamespaceAndPath("fastitemframes", "glow_item_frame");

	private static boolean resolved;
	private static @Nullable BlockEntityType<?> blockEntityType;
	private static @Nullable Block frameBlock;
	private static @Nullable Block glowFrameBlock;

	private FastItemFramesCompat() {
	}

	private static void resolve() {
		if (!resolved) {
			blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(BLOCK_ENTITY_ID).orElse(null);
			frameBlock = BuiltInRegistries.BLOCK.getOptional(BLOCK_ID).orElse(null);
			glowFrameBlock = BuiltInRegistries.BLOCK.getOptional(GLOW_BLOCK_ID).orElse(null);
			resolved = true;
		}
	}

	public static boolean isFrameBlock(BlockState state) {
		resolve();
		Block block = state.getBlock();
		return block != null && (block == frameBlock || block == glowFrameBlock);
	}

	public static boolean isFrameBlockEntity(BlockEntity blockEntity) {
		resolve();
		return blockEntityType != null && blockEntity.getType() == blockEntityType;
	}

	/** Direction from the frame block towards the block it hangs on, if {@code state} is a frame block. */
	public static Optional<Direction> towardsSupport(BlockState state) {
		if (!isFrameBlock(state) || !state.hasProperty(BlockStateProperties.FACING)) {
			return Optional.empty();
		}
		return Optional.of(state.getValue(BlockStateProperties.FACING).getOpposite());
	}

	public static ItemStack framedItem(BlockEntity blockEntity) {
		if (blockEntity instanceof Container container && container.getContainerSize() > 0) {
			return container.getItem(0);
		}
		return ItemStack.EMPTY;
	}
}

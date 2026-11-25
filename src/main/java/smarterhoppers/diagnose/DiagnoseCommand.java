package smarterhoppers.diagnose;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * {@code /smarterhoppers diagnose [radius]}: lists items waiting in hoppers because the chest that
 * holds them is full, and counts the marked containers and two-output hoppers nearby. Read-only.
 * Messages fall back to English for players without the mod.
 */
public final class DiagnoseCommand {
	static final int DEFAULT_RADIUS = 16;
	static final int MAX_RADIUS = 48;
	/** Waiting items listed in chat; the rest are only counted. */
	static final int MAX_LISTED = 10;

	private DiagnoseCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("smarterhoppers")
				.then(Commands.literal("diagnose")
						.executes(context -> run(context.getSource(), DEFAULT_RADIUS))
						.then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
								.executes(context -> run(context.getSource(), IntegerArgumentType.getInteger(context, "radius"))))));
	}

	private static int run(CommandSourceStack source, int radius) {
		BlockPos center = BlockPos.containing(source.getPosition());
		Diagnosis diagnosis = Diagnosis.scan(source.getLevel(), center, radius);
		source.sendSuccess(() -> Component.translatableWithFallback("commands.smarter_hoppers.diagnose.summary",
				"Within %s blocks: %s marked containers (%s feeding a tower), %s hoppers with two outputs, %s items waiting.",
				radius, diagnosis.markedContainers(), diagnosis.markedFeedingTowers(),
				diagnosis.twoOutputHoppers(), diagnosis.waiting().size()), false);
		diagnosis.waiting().stream().limit(MAX_LISTED).forEach(waiting -> {
			BlockPos pos = waiting.hopper();
			source.sendSuccess(() -> Component.translatableWithFallback("commands.smarter_hoppers.diagnose.waiting",
					"  %s × %s waiting at %s %s %s: the chest that holds it is full.",
					waiting.item().getCount(), waiting.item().getHoverName(), pos.getX(), pos.getY(), pos.getZ()), false);
		});
		if (diagnosis.waiting().size() > MAX_LISTED) {
			source.sendSuccess(() -> Component.translatableWithFallback("commands.smarter_hoppers.diagnose.more",
					"  ...and %s more.", diagnosis.waiting().size() - MAX_LISTED), false);
		}
		return diagnosis.waiting().size();
	}
}

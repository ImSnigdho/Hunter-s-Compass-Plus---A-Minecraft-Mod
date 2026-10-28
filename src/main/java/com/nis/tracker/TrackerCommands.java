package com.nis.tracker;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TrackerCommands {
	private TrackerCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tracker")
			.then(Commands.literal("sethunter")
				.then(Commands.argument("player", EntityArgument.player())
					.executes(TrackerCommands::setHunter)))
			.then(Commands.literal("setrunner")
				.then(Commands.argument("player", EntityArgument.player())
					.executes(TrackerCommands::setRunner)))
			.then(Commands.literal("start").executes(TrackerCommands::start))
			.then(Commands.literal("stop").executes(TrackerCommands::stop));

		LiteralCommandNode<CommandSourceStack> node = dispatcher.register(root);
		// Alias: redirect keeps full subcommand auto-complete.
		dispatcher.register(Commands.literal("t").redirect(node));
	}

	private static int setHunter(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
		if (p.getUUID().equals(ManhuntState.runner)) ManhuntState.runner = null;
		ManhuntState.hunters.add(p.getUUID());
		if (ManhuntState.running) TrackerCompass.lock(p);
		ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().name() + " is now a hunter."), true);
		return 1;
	}

	private static int setRunner(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
		ManhuntState.hunters.remove(p.getUUID());
		TrackerCompass.remove(p);
		ManhuntState.runner = p.getUUID();
		ManhuntState.resetTracking();
		ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().name() + " is now the runner."), true);
		return 1;
	}

	private static int start(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		if (ManhuntState.runner == null || ManhuntState.hunters.isEmpty()) {
			src.sendFailure(Component.literal("Set at least one hunter and a runner first."));
			return 0;
		}
		ManhuntState.resetTracking();
		ManhuntState.running = true;
		for (UUID id : ManhuntState.hunters) {
			ServerPlayer h = src.getServer().getPlayerList().getPlayer(id);
			if (h != null) TrackerCompass.lock(h);
		}
		src.sendSuccess(() -> Component.literal("Manhunt started."), true);
		return 1;
	}

	private static int stop(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
			TrackerCompass.remove(p);
		}
		ManhuntState.clearAll();
		src.sendSuccess(() -> Component.literal("Manhunt stopped; roles cleared."), true);
		return 1;
	}
}

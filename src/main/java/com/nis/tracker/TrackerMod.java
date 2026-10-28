package com.nis.tracker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class TrackerMod implements ModInitializer {
	public static final String MOD_ID = "manhunt_tracker";

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			TrackerCommands.register(dispatcher));

		// Tracking loop: portal logging, compass slot lock, lodestone updates.
		ServerTickEvents.END_SERVER_TICK.register(ManhuntState::tick);

		// Return the compass to slot 0 after the hunter dies and respawns.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if (ManhuntState.running && ManhuntState.hunters.contains(newPlayer.getUUID())) {
				TrackerCompass.lock(newPlayer);
			}
		});

		// Strip stray tracker compasses from anyone who isn't an active hunter.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var player = handler.getPlayer();
			if (!ManhuntState.running || !ManhuntState.hunters.contains(player.getUUID())) {
				TrackerCompass.remove(player);
			}
		});
	}
}

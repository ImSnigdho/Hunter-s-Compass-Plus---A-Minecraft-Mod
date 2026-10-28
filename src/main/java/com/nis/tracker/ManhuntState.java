package com.nis.tracker;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ManhuntState {
	public static final Set<UUID> hunters = new LinkedHashSet<>();
	public static UUID runner;
	public static boolean running;

	/** For each dimension: where the runner last LEFT it (portal coordinates, in that dimension). */
	private static final Map<ResourceKey<Level>, GlobalPos> portalLog = new HashMap<>();
	private static ResourceKey<Level> lastRunnerDim;
	private static BlockPos lastRunnerPos;

	private ManhuntState() {}

	public static void resetTracking() {
		portalLog.clear();
		lastRunnerDim = null;
		lastRunnerPos = null;
	}

	public static void clearAll() {
		hunters.clear();
		runner = null;
		running = false;
		resetTracking();
	}

	public static void tick(MinecraftServer server) {
		if (!running || runner == null) return;
		ServerPlayer runnerPlayer = server.getPlayerList().getPlayer(runner);

		if (runnerPlayer != null) {
			ResourceKey<Level> dim = runnerPlayer.level().dimension();
			if (lastRunnerDim != null && !dim.equals(lastRunnerDim)) {
				// Runner changed dimension: the last position seen in the old dimension is the portal they entered.
				// Leaving the End is intentionally not logged (no outward tracking from the End).
				if (!lastRunnerDim.equals(Level.END) && lastRunnerPos != null) {
					portalLog.put(lastRunnerDim, GlobalPos.of(lastRunnerDim, lastRunnerPos));
				}
			}
			if (!dim.equals(lastRunnerDim)) lastRunnerDim = dim;
			lastRunnerPos = runnerPlayer.blockPosition();
		}

		for (UUID hunterId : hunters) {
			ServerPlayer hunter = server.getPlayerList().getPlayer(hunterId);
			if (hunter == null) continue;
			TrackerCompass.lock(hunter);

			ResourceKey<Level> hunterDim = hunter.level().dimension();
			GlobalPos target = null;
			if (runnerPlayer != null && runnerPlayer.level().dimension().equals(hunterDim)) {
				target = GlobalPos.of(hunterDim, runnerPlayer.blockPosition());
			} else {
				target = portalLog.get(hunterDim);
			}
			if (target != null) TrackerCompass.point(hunter, target);
		}
	}
}

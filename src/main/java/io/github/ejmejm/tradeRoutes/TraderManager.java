package io.github.ejmejm.tradeRoutes;

import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraderManager {

    private static final long MISSION_CHECK_INTERVAL = 2 * 60 * 20; // 2 minutes
    private static Map<String, Instant> lastAffiliationTraderSpawnTime = new ConcurrentHashMap<>();

    /**
     * Fixes missions for all traders in the database.
     * This method iterates through all traders and calls fixMissions() on each one.
     * It ensures that all traders have the correct number and type of missions
     * according to the current configuration.
     */
    public static void FixAllTraderMissions() {
        TraderDatabase db = TraderDatabase.getInstance();
        for (Trader trader : db.getTraders().values()) {
            trader.fixMissions();
        }
    }

    public static void registerMissionCheckTask(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, TraderManager::FixAllTraderMissions, 5, MISSION_CHECK_INTERVAL);
    }

    public static void updateAffiliationTraderSpawnTime(String affiliation, Instant time) {
        lastAffiliationTraderSpawnTime.put(affiliation, time);
    }

    public static Duration getTimeSinceLastTraderSpawn(String affiliation) {
        Instant lastUpdate = lastAffiliationTraderSpawnTime.get(affiliation);
        if (lastUpdate == null) {
            return Duration.ofSeconds(Long.MAX_VALUE);
        }
        return Duration.between(lastUpdate, Instant.now());
    }

}

package io.github.ejmejm.tradeRoutes;

import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.plugin.Plugin;

public class MissionManager {

    private static final long MISSION_CHECK_INTERVAL = 2 * 60 * 20; // 2 minutes

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
                plugin, MissionManager::FixAllTraderMissions, 5, MISSION_CHECK_INTERVAL);
    }

}

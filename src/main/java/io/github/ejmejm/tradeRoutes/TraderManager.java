package io.github.ejmejm.tradeRoutes;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    public static void FixAllMissions() {
        TraderDatabase db = TraderDatabase.getInstance();

        // First fix all mission specs traders have
        for (Trader trader : db.getTraders().values()) {
            trader.fixMissions();
        }

        // Then fix all active missions
        for (ActiveTradeMission mission : db.getAllActiveTradeMissions()) {
            if (!mission.isValid()) {
                mission.failMission(Component.text(
                    "There was a problem with the mission, so it has been canceled.", NamedTextColor.RED));
            }
        }
    }

    public static void registerMissionCheckTask(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, TraderManager::FixAllMissions, 5, MISSION_CHECK_INTERVAL);
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

    /*
     * Fully removes a trader for the game, handling the database, FancyNpcs manager,
     * and related missions.
     */
    public static void removeTraderTransaction(Trader trader) throws SQLException {
        TraderDatabase traderDatabase = TraderDatabase.getInstance();
        try {
            traderDatabase.beginTransaction();

            // Remove trader from database
            traderDatabase.removeTrader(trader);

            // Remove all trade mission specs that this trader is the end trader for
            trader.removeAllMissions(true);

            // Remove all active trade missions for this trader
            List<ActiveTradeMission> activeMissions = traderDatabase.getActiveTradeMissionsByEndTrader(trader);
            for (ActiveTradeMission mission : activeMissions) {
                mission.failMission(Component.text(
                        "The trader at your destination has fallen. Mission failed.", NamedTextColor.RED));
            }

            traderDatabase.commitTransaction();

            // Remove trader from FancyNpcs manager
            if (trader.getNpc() != null) {
                trader.getNpc().removeForAll();
                FancyNpcsPlugin.get().getNpcManager().removeNpc(trader.getNpc());
            }
        } catch (Exception e) {
            traderDatabase.rollbackTransaction();
            throw new SQLException("Failed to remove trader: " + e.getMessage(), e);
        }
    }

}

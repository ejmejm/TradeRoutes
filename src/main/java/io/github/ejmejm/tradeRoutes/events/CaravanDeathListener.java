package io.github.ejmejm.tradeRoutes.events;

import io.github.ejmejm.tradeRoutes.TradeRoutes;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CaravanDeathListener implements Listener {

    NamespacedKey missionIdKey;
    
    public CaravanDeathListener() {
        missionIdKey = new NamespacedKey(TradeRoutes.getInstance(), "trade_mission_id");
    }


    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if the entity is part of the caravan for a mission
        Entity caravan = event.getEntity();
        Integer missionId = caravan.getPersistentDataContainer().get(missionIdKey, PersistentDataType.INTEGER);
        if (missionId == null) {
            return;
        }

        // Get the mission from the database
        Optional<ActiveTradeMission> mission;
        try {
            TraderDatabase db = TraderDatabase.getInstance();
            mission = db.getActiveTradeMissionById(missionId);
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe("Failed to process caravan death. "
                    + "mission could not be retrieved from the database: " + e.getMessage());
            return;
        }

        // Remove the key from the entity if the mission no longer exists
        if (mission.isEmpty()) {
            caravan.getPersistentDataContainer().remove(missionIdKey);
            return;
        }

        // Swap the normal drops with the required items
        List<ItemStack> drops = event.getDrops();
        drops.clear();
        drops.addAll(mission.get().getMissionSpec().getRequiredItems());

        // Fail the mission
        mission.get().failMission("Trade mission failed - your pack animal died!");
    }
}
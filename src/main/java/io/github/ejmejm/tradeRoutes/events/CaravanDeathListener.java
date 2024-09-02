package io.github.ejmejm.tradeRoutes.events;

import io.github.ejmejm.tradeRoutes.Constants;
import io.github.ejmejm.tradeRoutes.ItemUtils;
import io.github.ejmejm.tradeRoutes.TradeRoutes;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CaravanDeathListener implements Listener {

    private static final NamespacedKey MISSION_ID_KEY = new NamespacedKey(
            TradeRoutes.getInstance(), Constants.CARAVAN_MISSION_META_KEY);
    private static final NamespacedKey ITEM_DROPS_KEY = new NamespacedKey(
            TradeRoutes.getInstance(), Constants.DEATH_ITEMS_META_KEY);

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Check if we need to change drops
        String serializedDrops = entity.getPersistentDataContainer().get(ITEM_DROPS_KEY, PersistentDataType.STRING);
        if (serializedDrops != null) {
            List<ItemStack> drops = event.getDrops();
            drops.clear();
            List<ItemStack> newDrops = ItemUtils.deserializeItemStacks(serializedDrops);
            drops.addAll(newDrops);
        }

        // Check if the entity is part of the caravan for a mission
        Integer missionId = entity.getPersistentDataContainer().get(MISSION_ID_KEY, PersistentDataType.INTEGER);
        if (missionId != null) {
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

            // Check if the mission is still active (should always be the case or the tag would have been removed)
            if (mission.isPresent()) {
                // Send the player a message and play a sound
                UUID playerUUID = mission.get().getPlayerUUID();
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.7f);
                }

                // Fail the mission
                mission.get().failMission(
                        Component.text("Trade mission failed", NamedTextColor.DARK_RED)
                                .append(Component.text(" - your pack animal died!", NamedTextColor.WHITE)));
            } else {
                TradeRoutes.getInstance().getLogger().warning(
                        "Deceased entity had mission metadata, but the "
                                + "mission was not in the active mission database");
            }
        }
    }
}
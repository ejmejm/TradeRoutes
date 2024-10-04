package io.github.ejmejm.tradeRoutes.events;

import io.github.ejmejm.tradeRoutes.Constants;
import io.github.ejmejm.tradeRoutes.TradeRoutes;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.Optional;

public class CompassTrackingListener implements Listener {

    private static final NamespacedKey MISSION_ID_KEY = new NamespacedKey(
            TradeRoutes.getInstance(), Constants.CARAVAN_MISSION_META_KEY);
    private static final NamespacedKey TRACKING_KEY = new NamespacedKey(
            TradeRoutes.getInstance(), Constants.TRACKING_MISSION_KEY);

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Camel camel)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.COMPASS) {
            return;
        }

        // Cancel mounting when the player is holding a compass
        event.setCancelled(true);

        PersistentDataContainer camelData = camel.getPersistentDataContainer();
        Integer missionId = camelData.get(MISSION_ID_KEY, PersistentDataType.INTEGER);

        if (missionId == null) {
            player.sendMessage(Component.text(
                    "There is no mission to track for this camel.", NamedTextColor.YELLOW));
            return;
        }

        CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
        PersistentDataContainer compassData = compassMeta.getPersistentDataContainer();

        // If the compass is already tracking this mission, just send a message to the player and do nothing else
        Integer currentTrackedMissionId = compassData.get(TRACKING_KEY, PersistentDataType.INTEGER);
        if (currentTrackedMissionId != null && currentTrackedMissionId.equals(missionId)) {
            player.sendMessage(Component.text(
                "This compass is already tracking this mission destination.", NamedTextColor.YELLOW));
            return;
        }

        try {
            TraderDatabase db = TraderDatabase.getInstance();
            Optional<ActiveTradeMission> missionOpt = db.getActiveTradeMissionById(missionId);

            if (missionOpt.isPresent()) {
                ActiveTradeMission mission = missionOpt.get();
                Trader endTrader = mission.getMissionSpec().getEndTrader();

                compassMeta.setLodestone(endTrader.getLocation());
                compassMeta.setLodestoneTracked(false);
                compassData.set(TRACKING_KEY, PersistentDataType.INTEGER, missionId);
                item.setItemMeta(compassMeta);

                player.sendMessage(Component.text(
                        "Compass is now tracking the trade mission destination.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text(
                        "This trade mission is no longer valid.", NamedTextColor.RED));
            }
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe("Failed to process compass tracking: " + e.getMessage());
            player.sendMessage(Component.text(
                    "An error occurred while setting up tracking.", NamedTextColor.RED));
        }
    }
}
package io.github.ejmejm.tradeRoutes.events;

import io.github.ejmejm.tradeRoutes.Constants;
import io.github.ejmejm.tradeRoutes.MissionTracking;
import io.github.ejmejm.tradeRoutes.TradeRoutes;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class CompassDropListener implements Listener {

    private static final NamespacedKey TRACKING_KEY = new NamespacedKey(
            TradeRoutes.getInstance(), Constants.TRACKING_MISSION_KEY);

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        if (droppedItem.getType() == Material.COMPASS) {
            Integer missionId = droppedItem.getItemMeta().getPersistentDataContainer()
                    .get(TRACKING_KEY, PersistentDataType.INTEGER);
            
            if (missionId != null) {
                MissionTracking.removeCompassTracking(droppedItem);
            }
        }
    }
}
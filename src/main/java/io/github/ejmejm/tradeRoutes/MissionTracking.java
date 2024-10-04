package io.github.ejmejm.tradeRoutes;

import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

public class MissionTracking {

    private static final NamespacedKey TRACKING_KEY = new NamespacedKey(
            TradeRoutes.getInstance(), Constants.TRACKING_MISSION_KEY);

    public static void setCompassTracking(ItemStack compass, ActiveTradeMission mission) {
        if (compass.getType() != Material.COMPASS) return;
        
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        compassMeta.setLodestone(mission.getMissionSpec().getEndTrader().getLocation());
        compassMeta.setLodestoneTracked(false);
        compassMeta.getPersistentDataContainer().set(TRACKING_KEY, PersistentDataType.INTEGER, mission.getId());
        compass.setItemMeta(compassMeta);
    }

    public static void removeCompassTracking(ItemStack compass) {
        if (compass.getType() != Material.COMPASS) return;
        
        CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
        if (compassMeta.getPersistentDataContainer().has(TRACKING_KEY, PersistentDataType.INTEGER)) {
            compassMeta = (CompassMeta) new ItemStack(Material.COMPASS).getItemMeta();
            compass.setItemMeta(compassMeta);
        }
    }

    public static void removeTrackingFromPlayerInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                removeCompassTracking(item);
            }
        }
    }
}
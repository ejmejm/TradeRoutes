/**
 * Source: https://github.com/kangarko/CowCannon/blob/master/src/main/java/org/mineacademy/cowcannon/gui/MenuListener.java
 */

package io.github.ejmejm.tradeRoutes.events;

import io.github.ejmejm.tradeRoutes.TradeRoutes;
import io.github.ejmejm.tradeRoutes.gui.Button;
import io.github.ejmejm.tradeRoutes.gui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MenuListener implements Listener {

    public static final String IN_MENU_METADATA = "InTradeRoutesMenu";

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final int slot = event.getSlot();

        if (player.hasMetadata(IN_MENU_METADATA)) {
            final Menu menu = (Menu) player.getMetadata(IN_MENU_METADATA).getFirst().value();
            for (final Button button : menu.getButtons())
                if (button.getSlot() == slot) {
                    button.onClick(player);
                    event.setCancelled(true);
                }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        final Player player = (Player) event.getPlayer();
        if (player.hasMetadata(IN_MENU_METADATA)) {
            player.removeMetadata(IN_MENU_METADATA, TradeRoutes.getInstance());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (player.hasMetadata(IN_MENU_METADATA)) {
            player.removeMetadata(IN_MENU_METADATA, TradeRoutes.getInstance());
        }
    }
}
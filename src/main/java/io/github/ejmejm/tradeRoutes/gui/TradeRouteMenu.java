package io.github.ejmejm.tradeRoutes.gui;

import io.github.ejmejm.tradeRoutes.dataclasses.TradeMission;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TradeRouteMenu extends Menu {

    // Get all terracotta blocks
    private static Material[] TRADE_ROUTE_BLOCKS = {
            Material.WHITE_TERRACOTTA,
            Material.ORANGE_TERRACOTTA,
            Material.MAGENTA_TERRACOTTA,
            Material.LIGHT_BLUE_TERRACOTTA,
            Material.YELLOW_TERRACOTTA,
            Material.LIME_TERRACOTTA,
            Material.PINK_TERRACOTTA,
            Material.GRAY_TERRACOTTA,
            Material.LIGHT_GRAY_TERRACOTTA,
            Material.CYAN_TERRACOTTA,
            Material.PURPLE_TERRACOTTA,
            Material.BLUE_TERRACOTTA,
            Material.BROWN_TERRACOTTA,
            Material.GREEN_TERRACOTTA,
            Material.RED_TERRACOTTA,
            Material.BLACK_TERRACOTTA
    };

    public TradeRouteMenu(Trader trader) {

        //this.setSize(9*3);
        this.setTitle("Available Trade Requests");

        int maxTradeRoutes = 9 * 3;
        int tradeRouteCount = 0;

        // Add buttons for each trade destination
        for (TradeMission mission : trader.getTradeMissions()) {
            if (tradeRouteCount >= maxTradeRoutes) break;

            // Truncate the distance to an integer
            int distance = (int) mission.getStartTrader().getLocation().distance(mission.getEndTrader().getLocation());
            String affiliation = mission.getEndTrader().getAffiliation();
            String traderTitle;
            if (affiliation == null)
                traderTitle = "Unaffiliated Trader";
            else
                traderTitle = "Trader of " + affiliation;

            String coordinates = String.format("(%d, %d, %d)",
                    mission.getEndTrader().getLocation().getBlockX(),
                    mission.getEndTrader().getLocation().getBlockY(),
                    mission.getEndTrader().getLocation().getBlockZ());

            // Create the button for each trader
            this.addButton(new Button(tradeRouteCount) {

                @Override
                public ItemStack getItem() {
                    final ItemStack item = new ItemStack(Material.MAP);
                    final ItemMeta meta = item.getItemMeta();
                    meta.displayName(Component.text(traderTitle, NamedTextColor.GOLD));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Coords: " + coordinates, NamedTextColor.GRAY));
                    lore.add(Component.text("Distance: " + distance, NamedTextColor.DARK_GREEN));
                    lore.add(Component.newline());

                    // Add requested items
                    lore.add(Component.text("Requested Items:", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
                    for (ItemStack requestedItem : mission.getRequiredItems()) {
                        lore.add(Component.text("- " + requestedItem.getType(), NamedTextColor.BLUE)
                                .append(Component.text(" (" + requestedItem.getAmount() + ")", NamedTextColor.AQUA)));
                    }

                    // Add rewards
                    lore.add(Component.newline());
                    lore.add(Component.text("Rewards:", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
                    for (ItemStack requestedItem : mission.getRewards()) {
                        lore.add(Component.text("- " + requestedItem.getType(), NamedTextColor.BLUE)
                                .append(Component.text(" (" + requestedItem.getAmount() + ")", NamedTextColor.AQUA)));
                    }

                    meta.lore(lore);
                    item.setItemMeta(meta);
                    return item;
                }

                @Override
                public void onClick(Player player) {
                    player.sendMessage(NamedTextColor.GREEN + "Selected trader: " + affiliation + " at " + coordinates);
                    // Implement additional actions when a trader is selected, if needed
                }
            });

            tradeRouteCount++;
        }
    }

//    private class ListPlayersMenu extends Menu {
//
//        public ListPlayersMenu() {
//            super(TraderRouteMenu.this);
//
//            this.setSize(9 * 6);
//            this.setTitle("Listing Players");
//
//            int startingSlot = 0;
//
//            for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
//                this.addButton(new Button(startingSlot++) {
//                    @Override
//                    public ItemStack getItem() {
//                        final ItemStack item = new ItemStack(Material.PLAYER_HEAD);
//                        final SkullMeta meta = (SkullMeta) item.getItemMeta();
//                        meta.setDisplayName(ChatColor.WHITE + "Head Of " + onlinePlayer.getName());
//                        meta.setOwningPlayer(onlinePlayer);
//                        item.setItemMeta(meta);
//
//                        return item;
//                        //return ItemCreator.of(CompMaterial.PLAYER_HEAD).skullOwner(onlinePlayer.getName()).make();
//                    }
//
//                    @Override
//                    public void onClick(Player player) {
//                        player.sendMessage("You clicked player " + onlinePlayer.getName());
//                    }
//                });
//            }
//        }
//    }
}
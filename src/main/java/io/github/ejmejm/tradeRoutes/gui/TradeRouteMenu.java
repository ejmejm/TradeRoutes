package io.github.ejmejm.tradeRoutes.gui;

import io.github.ejmejm.tradeRoutes.TradeConfig;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import io.github.ejmejm.tradeRoutes.dataclasses.TradeMissionSpec;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TradeRouteMenu extends Menu {
    public TradeRouteMenu(Trader trader) {
        this.setSize(9 * 4);
        this.setTitle("Available Trade Requests");

        int maxTradeRoutes = 9 * 4;
        int tradeRouteCount = 0;

        // Add buttons for each trade destination
        for (TradeMissionSpec mission : trader.getTradeMissions()) {
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
                    lore.add(Component.text(""));

                    // Add requested items
                    lore.add(Component.text("Requested Items:", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
                    for (ItemStack requestedItem : mission.getRequiredItems()) {
                        lore.add(Component.text("- " + requestedItem.getType(), NamedTextColor.BLUE)
                                .append(Component.text(" (" + requestedItem.getAmount() + ")", NamedTextColor.AQUA)));
                    }
                    // Add rewards
                    lore.add(Component.text(""));
                    lore.add(Component.text("Rewards:", NamedTextColor.GOLD, TextDecoration.UNDERLINED));
                    for (ItemStack rewardItem : mission.getRewards()) {
                        lore.add(Component.text("- " + rewardItem.getType(), NamedTextColor.BLUE)
                                .append(Component.text(" (" + rewardItem.getAmount() + ")", NamedTextColor.AQUA)));
                    }

                    // Mark as taken if taken
                    if (mission.getTaken()) {
                        lore = lore.stream()
                                .map(component -> component
                                        .decoration(TextDecoration.STRIKETHROUGH, true)
                                        .color(NamedTextColor.GRAY)
                                )
                                .collect(Collectors.toList());

                        lore.add(Component.text(""));
                        lore.add(Component.text("TAKEN", NamedTextColor.RED, TextDecoration.BOLD));
                    }

                    Instant expirationTime = trader.getMissionExpirationTime(mission.getId());
                    if (expirationTime != null) {
                        lore.add(Component.text(""));
                        lore.add(Component.text("Refreshes in: " + getTimeUntil(expirationTime), NamedTextColor.YELLOW));
                    }
                    
                    meta.lore(lore);
                    item.setItemMeta(meta);
                    return item;
                }

                @Override
                public void onClick(Player player) {
                    checkMissionRequirements(player, trader, mission);
                }
            });

            tradeRouteCount++;
        }
    }

    private String getTimeUntil(Instant expirationTime) {
        Duration duration = Duration.between(Instant.now(), expirationTime);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return days + (days == 1 ? " day" : " days");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        } else {
            return "<1 minute";
        }
    }

    private void checkMissionRequirements(Player player, Trader trader, TradeMissionSpec mission) {
        if (mission.getTaken()) {
            player.sendMessage(Component.text(
                "This mission has already been taken by another player.", NamedTextColor.RED));
            return;
        }

        List<ItemStack> missingItems = new ArrayList<>();
        for (ItemStack requiredItem : mission.getRequiredItems()) {
            if (!player.getInventory().containsAtLeast(requiredItem, requiredItem.getAmount())) {
                missingItems.add(requiredItem);
            }
        }

        if (missingItems.isEmpty()) {
            new ConfirmMenu(trader, mission).displayTo(player);
        } else {
            Component message = Component.text("You are missing the following items:", NamedTextColor.RED);
            for (ItemStack item : missingItems) {
                message = message.append(Component.newline())
                        .append(Component.text("- " + item.getType(), NamedTextColor.DARK_BLUE)
                                .append(Component.text(" (" + item.getAmount() + ")", NamedTextColor.AQUA)));
            }
            player.sendMessage(message);
        }
    }

   private class ConfirmMenu extends Menu {

       public ConfirmMenu(Trader trader, TradeMissionSpec missionSpec) {
           this.setSize(9);
           this.setTitle("Accept Trade Request?");

            this.addButton(new Button(2) {
                @Override
                public ItemStack getItem() {
                    final ItemStack item = new ItemStack(Material.RED_TERRACOTTA);
                    final ItemMeta meta = item.getItemMeta();
                    meta.displayName(Component.text("Deny", NamedTextColor.RED));
                    item.setItemMeta(meta);
                    return item;
                }

                @Override
                public void onClick(Player player) {
                    TradeRouteMenu outerMenu = TradeRouteMenu.this;
                    outerMenu.displayTo(player);
                }
            });

            this.addButton(new Button(6) {
                @Override
                public ItemStack getItem() {
                    final ItemStack item = new ItemStack(Material.LIME_TERRACOTTA);
                    final ItemMeta meta = item.getItemMeta();
                    meta.displayName(Component.text("Confirm", NamedTextColor.GREEN));
                    item.setItemMeta(meta);
                    return item;
                }

                @Override
                public void onClick(Player player) {
                    initiateTradeMission(player, trader, missionSpec);
                    player.closeInventory();
                }
            });
       }

       private void initiateTradeMission(Player player, Trader trader, TradeMissionSpec missionSpec) {
           ActiveTradeMission mission;
           missionSpec.acquireLock();
           try {
               if (missionSpec.getTaken()) {
                   player.sendMessage(Component.text(
                           "This mission has already been taken by another player.", NamedTextColor.RED));
                   return;
               } else if (!missionSpec.isValid()) {
                   player.sendMessage(Component.text(
                           "This mission is invalid, try refreshing the menu.", NamedTextColor.RED));
                   return;
               }

               mission = ActiveTradeMission.initiateMission(player, missionSpec);
           } finally {
               missionSpec.releaseLock();
           }

            if (mission == null) {
                player.sendMessage(Component.text(
                        "Failed to initiate trade mission. Report this bug to an admin.", NamedTextColor.RED));
                return;
            }

           String affiliation = missionSpec.getEndTrader().getAffiliation();
           org.bukkit.Location location = missionSpec.getEndTrader().getLocation();
           String coords = String.format(
                   "(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());

           Component message = Component.text("You have accepted a trade mission! ")
                   .color(NamedTextColor.GREEN)
                   .append(Component.text("Your destination is the Trader of ")
                           .color(NamedTextColor.WHITE))
                   .append(Component.text(affiliation.isEmpty() ? "unaffiliated trader" : affiliation)
                           .color(NamedTextColor.GOLD))
                   .append(Component.text(" at ")
                           .color(NamedTextColor.WHITE))
                   .append(Component.text(coords)
                           .color(NamedTextColor.AQUA));

           player.sendMessage(message);

           // Announce mission to nearby players
           if (TradeConfig.getBool("announce_mission_starts")) {
               float announceDistance = TradeConfig.getFloat("announce_mission_distance");
               Location startLoc = missionSpec.getStartTrader().getLocation();
               Location endLoc = missionSpec.getEndTrader().getLocation();

               for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                   // Don't announce to the player who started the mission
                   if (nearbyPlayer.getUniqueId().equals(player.getUniqueId()))
                       continue;

                   if (nearbyPlayer.getLocation().distance(startLoc) <= announceDistance) {
                       int distanceToPlayer = (int) nearbyPlayer.getLocation().distance(player.getLocation());
                       Component announcement = Component.text(
                                       "A player has started a trade mission ", TextColor.color(159, 84, 255), TextDecoration.BOLD)

                               .append(Component.text(" <", NamedTextColor.BLUE))
                               .append(Component.text(
                                       distanceToPlayer, NamedTextColor.BLUE, TextDecoration.BOLD, TextDecoration.ITALIC))
                               .append(Component.text(" blocks from you>:", NamedTextColor.BLUE))

                               .append(Component.newline())
                               .append(Component.text(
                                       missionSpec.getStartTrader().getAffiliation(),
                                       NamedTextColor.GOLD, TextDecoration.BOLD, TextDecoration.ITALIC))
                               .append(
                                       Component.text(" (" + startLoc.getBlockX()
                                               + ", " + startLoc.getBlockY()
                                               + ", " + startLoc.getBlockZ()
                                               + ")", NamedTextColor.GRAY))

                               .append(Component.text(" -> ", NamedTextColor.WHITE))

                               .append(Component.text(
                                       missionSpec.getEndTrader().getAffiliation(),
                                       NamedTextColor.GOLD, TextDecoration.BOLD, TextDecoration.ITALIC))
                               .append(Component.text(" (" + endLoc.getBlockX() + ", "
                                       + endLoc.getBlockY()
                                       + ", " + endLoc.getBlockZ()
                                       + ")", NamedTextColor.GRAY));

                       nearbyPlayer.sendMessage(announcement);
                   }
               }

               player.sendMessage(Component.text(
                       "Your location and trade route have been announced to all players within " +
                               (int) announceDistance + " blocks", TextColor.color(159, 84, 255)));
           }
       }
   }
}
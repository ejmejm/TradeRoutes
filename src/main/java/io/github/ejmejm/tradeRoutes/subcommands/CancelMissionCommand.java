package io.github.ejmejm.tradeRoutes.subcommands;

import io.github.ejmejm.tradeRoutes.*;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CancelMissionCommand extends SubCommand {
    private static final String PERMISSION = "traderoutes.command.mission.cancel";

    @Override
    public String getName() {
        return "cancelmission";
    }

    @Override
    public String getDescription() {
        return "Cancels your current trade mission.";
    }

    @Override
    public String getSyntax() {
        return "/tr cancelmission";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(PERMISSION);
    }

    @RequireOneOfPermissions({PERMISSION})
    @ExpectPlayer
    @ExpectNArgs(1)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        TraderDatabase db = TraderDatabase.getInstance();

        try {
            Optional<ActiveTradeMission> missionOpt = db.getActiveTradeMissionByPlayer(player.getUniqueId());
            if (missionOpt.isEmpty()) {
                player.sendMessage(Component.text(
                        "You don't have an active trade mission to cancel.", NamedTextColor.RED));
                return;
            }

            CommandManager.setPendingConfirmation(player.getUniqueId(), ConfirmCommand.ConfirmationType.CANCEL_MISSION);
            player.sendMessage(Component.text(
                    "Are you sure you want to cancel your trade mission? This action cannot be undone. " +
                            "Type '/tr confirm' to confirm.",
                    NamedTextColor.YELLOW));

        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe(
                    "Failed to prepare mission cancellation: " + e.getMessage());
            player.sendMessage(Component.text(
                    "An error occurred while preparing to cancel your mission. Please contact an admin.",
                    NamedTextColor.RED
            ));
        }
    }
    
    public static void executeCancellation(Player player) {
        TraderDatabase db = TraderDatabase.getInstance();
        try {
            Optional<ActiveTradeMission> missionOpt = db.getActiveTradeMissionByPlayer(player.getUniqueId());
            if (missionOpt.isEmpty()) {
                player.sendMessage(Component.text(
                        "You don't have an active trade mission to cancel.", NamedTextColor.RED));
                return;
            }

            ActiveTradeMission mission = missionOpt.get();
            Entity entity = Bukkit.getEntity(mission.getCaravanUUID());
            
            if (entity != null) {
                // Remove mission ID tag
                NamespacedKey missionKey = new NamespacedKey(
                        TradeRoutes.getInstance(), Constants.CARAVAN_MISSION_META_KEY);
                PersistentDataContainer container = entity.getPersistentDataContainer();
                container.remove(missionKey);

                if (entity instanceof Mob caravan) {
                    // Update caravan properties
                    String affiliation = mission.getMissionSpec().getEndTrader().getAffiliation();
                    caravan.setRemoveWhenFarAway(true);
                    caravan.customName(Component.text("Abandoned", NamedTextColor.DARK_RED)
                            .append(Component.text(" Caravan from ", NamedTextColor.WHITE))
                            .append(Component.text(affiliation, NamedTextColor.GOLD)));
                    caravan.setLeashHolder(null);
                }
            }

            // Fail the mission
            mission.failMission(Component.text(
                    "You have abandoned your mission ", NamedTextColor.DARK_RED)
                    .append(Component.text(
                            "- The caravan may despawn if a player is not around it.", NamedTextColor.WHITE)));
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe("Failed to cancel mission: " + e.getMessage());
            player.sendMessage(Component.text(
                    "An error occurred while cancelling your mission. Please contact an admin.",
                    NamedTextColor.RED
            ));
        }
    }
}
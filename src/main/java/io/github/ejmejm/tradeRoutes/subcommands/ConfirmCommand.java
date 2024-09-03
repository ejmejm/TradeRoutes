package io.github.ejmejm.tradeRoutes.subcommands;

import io.github.ejmejm.tradeRoutes.CommandManager;
import io.github.ejmejm.tradeRoutes.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;

public class ConfirmCommand extends SubCommand {
    private static final String PERMISSION = "traderoutes.command.confirm";

    private static final int CONFIRMATION_TIMEOUT = 8; // 120; // seconds

    public enum ConfirmationType {
        CANCEL_MISSION,
        NONE
    }

    @Override
    public String getName() {
        return "confirm";
    }

    @Override
    public String getDescription() {
        return "Confirms the pending action.";
    }

    @Override
    public String getSyntax() {
        return "/tr confirm";
    }

    @RequireOneOfPermissions({PERMISSION})
    @ExpectPlayer
    @ExpectNArgs(1)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        CommandManager.PendingConfirmation pendingConfirmation = CommandManager.getPendingConfirmation(player.getUniqueId());

        if (pendingConfirmation != null && 
            Duration.between(pendingConfirmation.timestamp(), Instant.now()).toSeconds() <= CONFIRMATION_TIMEOUT) {

            switch (pendingConfirmation.type()) {
                case ConfirmationType.CANCEL_MISSION:
                    CancelMissionCommand.executeCancellation(player);
                    break;
                default:
                    player.sendMessage(Component.text("Unknown confirmation type.", NamedTextColor.RED));
                    return;
            }
            
            CommandManager.clearPendingConfirmation(player.getUniqueId());
        } else {
            if (pendingConfirmation == null) {
                player.sendMessage(Component.text("There is no pending action to confirm.", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("The confirmation request has expired. Please try the original command again.", NamedTextColor.RED));
                CommandManager.clearPendingConfirmation(player.getUniqueId());
            }
        }
    }
}
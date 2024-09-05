package io.github.ejmejm.tradeRoutes.subcommands;

import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RefreshMissionsCommand extends SubCommand {
    private static final String PERMISSION = "traderoutes.command.trader.refresh";

    @Override
    public String getName() {
        return "refreshmissions";
    }

    @Override
    public String getDescription() {
        return "Refreshes missions for a trader or for all traders if no trader is specified.";
    }

    @Override
    public String getSyntax() {
        return "/tr refreshmissions (trader_affiliation)";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(PERMISSION);
    }

    private void refreshTraderMissions(Player player, String affiliation) {
        try {
            TraderDatabase traderDB = TraderDatabase.getInstance();
            Optional<Trader> traderOpt = traderDB.getTraderByAffiliation(affiliation);
            
            if (traderOpt.isEmpty()) {
                player.sendMessage(Component.text("No trader found with the affiliation: " + affiliation, CMD_ERROR_COLOR));
                return;
            }

            traderOpt.get().refreshMissions();
            player.sendMessage(Component.text("Missions refreshed for trader: " + affiliation, CMD_SUCCESS_COLOR));
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(Component.text("An error occurred while refreshing missions.", CMD_ERROR_COLOR));
        }
    }

    private void refreshAllTraderMissions(Player player) {
        TraderDatabase traderDB = TraderDatabase.getInstance();
        Map<String, Trader> traders = traderDB.getTraders();

        for (Trader trader : traders.values()) {
            trader.refreshMissions();
        }

        player.sendMessage(Component.text("Missions refreshed for all traders.", CMD_SUCCESS_COLOR));
    }

    @RequireOneOfPermissions({PERMISSION})
    @ExpectPlayer
    @ExpectNArgsRange(min = 1, max = 2)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 2) {
            String affiliation = args[1];
            refreshTraderMissions(player, affiliation);
        } else {
            refreshAllTraderMissions(player);
        }
    }
}
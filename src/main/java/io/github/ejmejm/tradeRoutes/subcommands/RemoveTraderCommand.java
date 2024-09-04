package io.github.ejmejm.tradeRoutes.subcommands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import io.github.ejmejm.tradeRoutes.PluginChecker;
import io.github.ejmejm.tradeRoutes.TradeRoutes;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RemoveTraderCommand extends SubCommand {
    private static final String BASE_PERMISSION = "traderoutes.command.trader.remove.town";
    private static final String ANY_AFFILIATION_PERMISSION = "traderoutes.command.trader.remove.any";
    private static final Lock traderRemovalLock = new ReentrantLock();

    @Override
    public String getName() {
        return "removetrader";
    }

    @Override
    public String getDescription() {
        return "Removes a trader.";
    }

    @Override
    public String getSyntax() {
        return "/tr removetrader (affiliation)";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(BASE_PERMISSION, ANY_AFFILIATION_PERMISSION);
    }

    private void removeTrader(Player player) throws SQLException {
        PluginChecker pluginChecker = PluginChecker.getInstance();
        TraderDatabase traderDatabase = TraderDatabase.getInstance();

        traderRemovalLock.lock();
        try {
            // Remove unaffiliated trader if Towny is not enabled
            if (!pluginChecker.isPluginEnabled("Towny")) {
                // Check if the user has the ANY_AFFILIATION_PERMISSION,
                // and if so, check if they are standing right next to a trader
                if (player.hasPermission(ANY_AFFILIATION_PERMISSION)) {
                    for (Trader trader : traderDatabase.getTraders().values()) {
                        if (trader.getLocation().distance(player.getLocation()) < 2) {
                            removeTraderTransaction(trader);
                            
                            player.sendMessage(Component.text(
                                    "Trader with affiliation ", CMD_SUCCESS_COLOR)
                                    .append(Component.text(trader.getAffiliation(), NamedTextColor.GOLD))
                                    .append(Component.text(" has been removed", CMD_SUCCESS_COLOR)));
                            return;
                        }
                    }
                    player.sendMessage(Component.text(
                            "You need to stand directly next to a trader to remove them", CMD_ERROR_COLOR));
                } else {
                    player.sendMessage(Component.text(
                            "You do not have permission to remove traders!", CMD_ERROR_COLOR));
                }
                return;
            }

            TownyAPI towny = TownyAPI.getInstance();

            // Otherwise make sure the user is in a town
            Town playerTown = towny.getTown(player);
            if (playerTown == null) {
                player.sendMessage(Component.text(
                        "You need to be part of a town to remove a trader", CMD_ERROR_COLOR));
                if (player.hasPermission(ANY_AFFILIATION_PERMISSION)) {
                    player.sendMessage(Component.text(
                            "Or you can specify an affiliation at the end of your command "
                            + "(/tr removetrader <affiliation>)", CMD_ERROR_COLOR));
                }
                return;
            }

            Optional<Trader> traderOpt = traderDatabase.getTraderByAffiliation(playerTown.getName());

            if (traderOpt.isPresent()) {
                Trader trader = traderOpt.get();
                removeTraderTransaction(trader);

                player.sendMessage(Component.text(
                        "Trader with affiliation ", CMD_SUCCESS_COLOR)
                        .append(Component.text(trader.getAffiliation(), NamedTextColor.GOLD))
                        .append(Component.text(" has been removed", CMD_SUCCESS_COLOR)));
            } else {
                player.sendMessage(Component.text(
                        "Your town does not have a trader", CMD_ERROR_COLOR));
            }

        } finally {
            traderRemovalLock.unlock();
        }
    }

    private void removeTrader(String affiliation, Player player) throws SQLException {
        TraderDatabase traderDatabase = TraderDatabase.getInstance();
        
        traderRemovalLock.lock();
        try {
            Optional<Trader> traderOpt = traderDatabase.getTraderByAffiliation(affiliation);
            
            if (traderOpt.isEmpty()) {
                player.sendMessage(Component.text(
                        "No trader with affiliation ", CMD_ERROR_COLOR)
                        .append(Component.text(affiliation, NamedTextColor.GOLD))
                        .append(Component.text(" found", CMD_ERROR_COLOR)));
                return;
            }

            Trader trader = traderOpt.get();
            removeTraderTransaction(trader);

            player.sendMessage(Component.text(
                    "Trader with affiliation ", CMD_SUCCESS_COLOR)
                    .append(Component.text(trader.getAffiliation(), NamedTextColor.GOLD))
                    .append(Component.text(" has been removed", CMD_SUCCESS_COLOR))); 
        } finally {
            traderRemovalLock.unlock();
        }
    }

    private void removeTraderTransaction(Trader trader) throws SQLException {
        TraderDatabase traderDatabase = TraderDatabase.getInstance();
        try {
            traderDatabase.beginTransaction();

            traderDatabase.removeTrader(trader);
            
            if (trader.getNpc() != null) {
                trader.getNpc().removeForAll();
                FancyNpcsPlugin.get().getNpcManager().removeNpc(trader.getNpc());
            }
            
            traderDatabase.commitTransaction();
        } catch (Exception e) {
            traderDatabase.rollbackTransaction();
            throw new SQLException("Failed to remove trader: " + e.getMessage(), e);
        }
    }

    @RequireOneOfPermissions({BASE_PERMISSION, ANY_AFFILIATION_PERMISSION})
    @ExpectPlayer
    @ExpectNArgsRange(min = 1, max = 2)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        if (args.length == 2 && !sender.hasPermission(ANY_AFFILIATION_PERMISSION)) {
            sender.sendMessage(Component.text(
                    "You do not have permission to remove traders with arbitrary affiliation", CMD_ERROR_COLOR));
            sender.sendMessage(Component.text(
                    "You can only remove a trader in your own town with '/tr removetrader'", CMD_ERROR_COLOR));
            return;
        }

        Player player = (Player) sender;

        try {
            if (args.length == 2) {
                String affiliation = args[1];
                removeTrader(affiliation, player);
            } else {
                removeTrader(player);
            }
        } catch (SQLException e) {
            sender.sendMessage(Component.text(
                    "Command failed, report this bug to an admin", CMD_ERROR_COLOR));
            TradeRoutes.getInstance().getLogger().severe("Failed to remove trader: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
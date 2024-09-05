package io.github.ejmejm.tradeRoutes.subcommands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import io.github.ejmejm.tradeRoutes.PluginChecker;
import io.github.ejmejm.tradeRoutes.TradeConfig;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.TraderManager;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class SpawnTraderCommand extends SubCommand {
    private static final double maxSpawnDistance = 128;
    private static final String BASE_PERMISSION = "traderoutes.command.trader.spawn.town";
    private static final String ANY_AFFILIATION_PERMISSION = "traderoutes.command.trader.spawn.any";

    @Override
    public String getName() {
        return "spawntrader";
    }

    @Override
    public String getDescription() {
        return "Spawns a trader.";
    }

    @Override
    public String getSyntax() {
        return "/tr spawntrader (affiliation)";
    }

    @Override
    public List<String> getPermissions() {
        return List.of(BASE_PERMISSION, ANY_AFFILIATION_PERMISSION);
    }

    private void createTraderNPC(Location spawnLoc, Player player) {
        PluginChecker pluginChecker = PluginChecker.getInstance();

        // Create an unaffiliated trader if Towny is not enabled
        if (!pluginChecker.isPluginEnabled("Towny")) {
            Trader.createAndSpawn(spawnLoc, null, player);
            return;
        }

        TownyAPI towny = TownyAPI.getInstance();

        // Otherwise make sure the user is in their town
        Town playerTown = towny.getTown(player);
        if (playerTown == null) {
            player.sendMessage(Component.text(
                    "You need to be part of a town to spawn a trader", CMD_ERROR_COLOR));
            if (player.hasPermission(ANY_AFFILIATION_PERMISSION)) {
                player.sendMessage(Component.text(
                        "Or you can specify an affiliation at the end of your command "
                        + "(/tr spawntrader <affiliation>)", CMD_ERROR_COLOR));
            }
            return;
        }

        // Check if the player is trying to spawn the trader in their town
        Town spawnLocTown = towny.getTown(spawnLoc);
        if (spawnLocTown == null || !spawnLocTown.equals(playerTown)) {
            player.sendMessage(Component.text("You can only spawn a trader in your town "
                    + "(the trader spawns where you are looking)", CMD_ERROR_COLOR));
            return;
        }

        // Check if a trader already exists for the town
        try {
            TraderDatabase traderDB = TraderDatabase.getInstance();
            if (traderDB.traderWithAffiliationExists(playerTown.getName())) {
                player.sendMessage(Component.text("Your town already has a trader", CMD_ERROR_COLOR));
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Check cooldown before spawning
        Duration timeSinceLastSpawn = TraderManager.getTimeSinceLastTraderSpawn(playerTown.getName());
        int cooldownMinutes = TradeConfig.getInt("spawn_trader_cooldown");
        if (timeSinceLastSpawn.toMinutes() < cooldownMinutes) {
            long remainingMinutes = cooldownMinutes - timeSinceLastSpawn.toMinutes();
            player.sendMessage(Component.text(
                    "You need to wait " + remainingMinutes + " more minutes before spawning another trader.",
                    CMD_ERROR_COLOR));
            return;
        }

        Trader.createAndSpawn(spawnLoc, playerTown.getName(), player);
        TraderManager.updateAffiliationTraderSpawnTime(playerTown.getName(), Instant.now());
    }

    private void createTraderNPC(Location spawnLoc, Player player, String affiliation) {
        Trader.createAndSpawn(spawnLoc, affiliation, player);
    }

    @RequireOneOfPermissions({BASE_PERMISSION, ANY_AFFILIATION_PERMISSION})
    @ExpectPlayer
    @ExpectNArgsRange(min = 1, max = 2)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        if (args.length == 2 && !sender.hasPermission(ANY_AFFILIATION_PERMISSION)) {
            sender.sendMessage(Component.text(
                    "You do not have permission to spawn traders with arbitrary affiliation!", CMD_ERROR_COLOR));
            sender.sendMessage(Component.text(
                    "You can only spawn a trader in your own town with /tr spawntrader", CMD_ERROR_COLOR));
            return;
        }

        Player player = (Player) sender;
        RayTraceResult rayHit = player.rayTraceBlocks(maxSpawnDistance, FluidCollisionMode.NEVER);

        if (rayHit == null) {
            sender.sendMessage(Component.text(
                    "You need to be looking at a block when you use this command!", CMD_ERROR_COLOR));
            return;
        }

        World world = player.getWorld();
        Vector hitPos = rayHit.getHitPosition();
        Location spawnLoc = new Location(world, hitPos.getBlockX(), hitPos.getBlockY(), hitPos.getBlockZ());

        if (args.length == 2) {
            String affiliation = args[1];
            createTraderNPC(spawnLoc, player, affiliation);
        } else {
            createTraderNPC(spawnLoc, player);
        }
    }
}

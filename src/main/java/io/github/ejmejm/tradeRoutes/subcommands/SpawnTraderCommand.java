package io.github.ejmejm.tradeRoutes.subcommands;

import io.github.ejmejm.tradeRoutes.SubCommand;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SpawnTraderCommand extends SubCommand {
    private static final double maxSpawnDistance = 128;

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

    private UUID genBlankUUID() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private void createTraderNPC(Location spawnLoc, Player player) {
        Trader.createAndSpawn(spawnLoc, null, player);
    }

    @ExpectPlayer
    @ExpectNArgsRange(min = 1, max = 2)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        RayTraceResult rayHit = player.rayTraceBlocks(maxSpawnDistance, FluidCollisionMode.NEVER);

        if (rayHit == null) {
            sender.sendMessage(CMD_ERROR_COLOR + "You need to be looking at a block when you use this command!");
            return;
        }

        World world = player.getWorld();
        Vector hitPos = rayHit.getHitPosition();
        Location spawnLoc = new Location(world, hitPos.getBlockX(), hitPos.getBlockY(), hitPos.getBlockZ());

        createTraderNPC(spawnLoc, player);
    }
}

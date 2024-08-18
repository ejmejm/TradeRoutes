package io.github.ejmejm.tradeRoutes.subcommands;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.utils.SkinFetcher;
import io.github.ejmejm.tradeRoutes.SubCommand;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.UUID;

public class SpawnTraderCommand extends SubCommand {
    private static final String TRADER_NAME_COLOR = "<#8a44bd>";
    private static double maxSpawnDistance = 128;

    @Override
    public String getName() {
        return "spawntrader";
    }

    @Override
    public String getDescription() {
        return "Spawn a trader.";
    }

    @Override
    public String getSyntax() {
        return "/tr spawntrader <affiliation>";
    }

    private UUID genBlankUUID() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private void createTraderNPC(Location spawnLoc, Player player) {
        FancyNpcsPlugin plugin = FancyNpcsPlugin.get();

        String traderName = Trader.TRADER_NAME_PREFIX + UUID.randomUUID();
        NpcData data = new NpcData(traderName, player.identity().uuid(), spawnLoc);

        // Purple name
        data.setDisplayName(TRADER_NAME_COLOR + "<bold>Town Trader<reset> <gray><i>(Click me!)</i>");
        data.setTurnToPlayer(true);

        SkinFetcher skin = new SkinFetcher("https://s.namemc.com/i/0c451c5e949f8806.png");
        data.setSkin(skin);

        Npc npc = plugin.getNpcAdapter().apply(data);
        Trader trader = new Trader(npc, null);

        // Add trader to database
        try {
            TraderDatabase.getInstance().addTrader(trader);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Register and spawn NPC
        plugin.getNpcManager().registerNpc(npc);
        npc.create();
        npc.spawnForAll();
    }

    @ExpectPlayer
    @ExpectNArgsRange(min = 1, max = 2)
    @Override
    protected void perform(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        RayTraceResult rayHit = player.rayTraceBlocks(maxSpawnDistance, FluidCollisionMode.NEVER);

        if (rayHit == null) {
            sender.sendMessage("You need to be looking at a block when you use this command!");
            return;
        }

        World world = player.getWorld();
        Vector hitPos = rayHit.getHitPosition();
        Location spawnLoc = new Location(world, hitPos.getBlockX(), hitPos.getBlockY(), hitPos.getBlockZ());

        createTraderNPC(spawnLoc, player);
    }
}

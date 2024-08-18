package io.github.ejmejm.tradeRoutes;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.utils.SkinFetcher;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class SpawnTraderCommand implements CommandExecutor {

    static double maxSpawnDistance = 128;

    private UUID genBlankUUID() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

//    private Entity createTraderNPC(Location spawnLoc) {
//        Villager traderNPC = spawnLoc.getWorld().spawn(spawnLoc, Villager.class);
//        UUID traderId = traderNPC.getUniqueId();
//
//        traderNPC.setCustomName(ChatColor.RED + "Town Trader");
//        traderNPC.setCustomNameVisible(true);
//
//        traderNPC.setPersistent(true);
//        traderNPC.setNoDamageTicks(Integer.MAX_VALUE);
//
//        return traderNPC;
//    }

    private Npc createTraderNPC(Location spawnLoc, Player player) {
        FancyNpcsPlugin plugin = FancyNpcsPlugin.get();

        String traderName = Trader.TRADER_NAME_PREFIX + UUID.randomUUID();
        NpcData data = new NpcData(traderName, player.identity().uuid(), spawnLoc);

        // Purple name
        data.setDisplayName("<#8a44bd><bold>Town Trader<reset> <gray><i>(Click me!)</i>");
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

        return npc;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        } else if (args.length > 1) {
            return false;
        }

        Player player = (Player) sender;
        RayTraceResult rayHit = player.rayTraceBlocks(maxSpawnDistance, FluidCollisionMode.NEVER);

        if (rayHit == null) {
            sender.sendMessage("You need to be looking at a block when you use this command!");
            return true;
        }

        World world = player.getWorld();
        Vector hitPos = rayHit.getHitPosition();
        Location spawnLoc = new Location(world, hitPos.getBlockX(), hitPos.getBlockY(), hitPos.getBlockZ());

        createTraderNPC(spawnLoc, player);

        return true;
    }
}

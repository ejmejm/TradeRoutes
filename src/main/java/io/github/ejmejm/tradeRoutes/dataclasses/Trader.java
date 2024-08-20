package io.github.ejmejm.tradeRoutes.dataclasses;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.utils.SkinFetcher;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.gui.TradeRouteMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Trader {
    public static String TRADER_NAME_PREFIX = "Trader-";
    private static final String SKIN_URL = "https://www.minecraftskins.net/therevenant/download";
    private static final String TRADER_NAME_COLOR = "<#8a44bd>";
    private int maxTradeRoutes = 5;

    private final Npc npc;
    private String affiliation; // town name

    public Trader(String uuid, String affiliation) {
        // Throw an exception if the NPC does not exist
        this.npc = FancyNpcsPlugin.get().getNpcManager().getNpcById(uuid);
        if (this.npc == null) {
            throw new IllegalArgumentException("NPC with UUID " + uuid + " does not exist.");
        }
        this.affiliation = affiliation;
    }

    public Trader(Npc npc, String affiliation) {
        this.npc = npc;
        this.affiliation = affiliation;
    }

    public static Trader createAndSpawn(@NotNull Location spawnLoc, String affiliation, Player creator) {
        FancyNpcsPlugin plugin = FancyNpcsPlugin.get();

        String traderName = Trader.TRADER_NAME_PREFIX + UUID.randomUUID();
        UUID creatorId = creator == null ? null : creator.getUniqueId();
        NpcData data = new NpcData(traderName, creatorId, spawnLoc);

        // Purple name
        data.setDisplayName(TRADER_NAME_COLOR + "<bold>Town Trader<reset> <gray><i>(Click me!)</i>");
        data.setTurnToPlayer(true);

        SkinFetcher skin = new SkinFetcher(SKIN_URL);
        data.setSkin(skin);
        Npc npc = plugin.getNpcAdapter().apply(data);
        Trader trader = new Trader(npc, null);

        trader.getNpc().getData().setOnClick(player -> {
            new TradeRouteMenu(trader).displayTo(player);
        });

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

        return trader;
    }

    public static Trader createAndSpawnFromData(NpcData data) {
        FancyNpcsPlugin plugin = FancyNpcsPlugin.get();
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

        return trader;
    }

    public List<TradeMission> getTradeMissions() {
        // TODO: Implement persistent missions instead of random

        HashMap<String, Trader> traders = TraderDatabase.getInstance().getTraders();
        traders.remove(this.getUUID());

        List<Trader> sortedTraders = traders.values().stream()
                .sorted(Comparator.comparingDouble(t -> t.getLocation().distance(this.getLocation())))
                .limit(maxTradeRoutes)
                .toList();

        return sortedTraders.stream()
                .map(trader -> TradeMission.createRandomMission(this, trader))
                .toList();
    }

    public Npc getNpc() {
        return npc;
    }
    public String getAffiliation() {
        return affiliation;
    }
    public String getUUID() {
        return npc.getData().getId();
    }
    public String getName() {
        return npc.getData().getName();
    }
    public Location getLocation() {
        return npc.getData().getLocation();
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
}

package io.github.ejmejm.tradeRoutes.dataclasses;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.utils.SkinFetcher;
import io.github.ejmejm.tradeRoutes.TradeRoutes;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.gui.TradeRouteMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

@DatabaseTable(tableName = "traders")
public class Trader {
    public static String TRADER_NAME_PREFIX = "Trader-";
    private static final String SKIN_URL = "https://www.minecraftskins.net/therevenant/download";
    private static final String TRADER_NAME_COLOR = "<#8a44bd>";
    private int maxTradeRoutes = 30;

    @DatabaseField(id = true)
    private String uuid;

    @DatabaseField
    private String affiliation;

    // No-args constructor required by ORMLite
    public Trader() {}

    public Trader(String uuid, String affiliation) {
        this.uuid = uuid;
        this.affiliation = affiliation;
        connectToNpcManager();
    }

    public Trader(Npc npc, String affiliation) {
        this.uuid = npc.getData().getId();
        this.affiliation = affiliation;
        connectToNpcManager();
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
        Trader trader = new Trader(npc, affiliation);

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
        plugin.getNpcManager().saveNpcs(false);

        return trader;
    }

    private void onClick(Player player) {
        new TradeRouteMenu(this).displayTo(player);
    }

    public boolean connectToNpcManager() {
        Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpcById(uuid);
        if (npc == null) {
            TradeRoutes.getInstance().getLogger().warning("NPC with UUID " + uuid + " does not exist in the NpcManager.");
            return false;
        } else {
            npc.getData().setOnClick(this::onClick);
            return true;
        }
    }

    public List<TradeMission> getTradeMissions() {
        // TODO: Implement persistent missions instead of random

        Map<String, Trader> traders = new HashMap<>(TraderDatabase.getInstance().getTraders());
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
        return FancyNpcsPlugin.get().getNpcManager().getNpcById(uuid);
    }
    public String getName() {
        return getNpc().getData().getName();
    }
    public Location getLocation() {
        return getNpc().getData().getLocation();
    }
    public String getAffiliation() {
        return affiliation;
    }
    public String getUUID() { return uuid; }

    public void setAffiliation(String affiliation) { this.affiliation = affiliation; }
    public void setUUID(String uuid) { this.uuid = uuid; }

}

package io.github.ejmejm.tradeRoutes;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcManager;
import io.github.ejmejm.tradeRoutes.dataclasses.ActiveTradeMission;
import io.github.ejmejm.tradeRoutes.dataclasses.TradeMissionSpec;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TraderDatabase {
    private static TraderDatabase INSTANCE;
    private static ConnectionSource connectionSource;
    private static Dao<Trader, String> traderDao;
    private static Dao<ActiveTradeMission, Integer> activeTradeMissionDao;
    private static Dao<TradeMissionSpec, Integer> tradeMissionSpecDao;
    private static NpcManager npcManager;
    private static Plugin plugin;

    private TraderDatabase(String path, Plugin plugin) throws SQLException {
        // Connect to the database
        connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + path);
        traderDao = DaoManager.createDao(connectionSource, Trader.class);
        tradeMissionSpecDao = DaoManager.createDao(connectionSource, TradeMissionSpec.class);
        activeTradeMissionDao = DaoManager.createDao(connectionSource, ActiveTradeMission.class);

        // Create tables if they don't exist
        TableUtils.createTableIfNotExists(connectionSource, Trader.class);
        TableUtils.createTableIfNotExists(connectionSource, TradeMissionSpec.class);
        TableUtils.createTableIfNotExists(connectionSource, ActiveTradeMission.class);

        TraderDatabase.plugin = plugin;
        npcManager = FancyNpcsPlugin.get().getNpcManager();

        if (!syncDatabase()) {
            throw new SQLException("Failed to sync database with NPC registry.");
        }
    }

    public static void initialize(String path, Plugin plugin) throws SQLException {
        if (INSTANCE == null) {
            INSTANCE = new TraderDatabase(path, plugin);
        } else {
            throw new IllegalStateException("TraderDatabase has already been initialized.");
        }
    }

    public static TraderDatabase getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("TraderDatabase has not been initialized.");
        }
        return INSTANCE;
    }

    private boolean syncDatabase() {
        Map<String, Npc> npcMap = npcManager.getAllNpcs().stream()
                .filter(npc -> npc.getData().getName().startsWith(Trader.TRADER_NAME_PREFIX))
                .collect(Collectors.toMap(npc -> npc.getData().getId(), npc -> npc));

        Logger logger = plugin.getLogger();

        try {
            List<Trader> traders = traderDao.queryForAll();
            for (Trader trader : traders) {
                String uuid = trader.getUUID();
                if (!npcMap.containsKey(uuid)) {
                    logger.warning("NPC with UUID " + uuid + " does not exist in the NpcRegistry. Removing from database.");
                    traderDao.delete(trader);
                } else {
                    trader.connectToNpcManager();
                    npcMap.remove(uuid);
                }
            }

            if (!npcMap.isEmpty()) {
                logger.warning("The following NPCs are in the NpcRegistry but not in the database:");
                npcMap.forEach((uuid, npc) -> logger.warning(uuid));
                logger.warning("Removing them from the NpcRegistry, which will remove them from the server.");

                for (Npc npc : npcMap.values()) {
                    npc.removeForAll();
                    npcManager.removeNpc(npc);
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to sync database with NPC registry: " + e);
            return false;
        }

        return true;
    }
    public void closeConnection() throws Exception {
        if (connectionSource != null && (connectionSource.isOpen("traders")
                        || connectionSource.isOpen("active_trade_missions")
                        || connectionSource.isOpen("trade_mission_specs")
            )) {
            connectionSource.close();
        }
    }

    /*****************
     *    Traders    *
     *****************/

    public void addTrader(Trader trader) throws SQLException {
        traderDao.create(trader);
    }

    public void removeTrader(String traderUUID) throws SQLException {
        traderDao.deleteById(traderUUID);
    }

    // TODO: When a trader is removed, make sure to remove all mission specs and active missions associated with them
    public void removeTrader(Trader trader) throws SQLException {
        traderDao.delete(trader);
    }

    public boolean updateTrader(Trader trader) throws SQLException {
        return traderDao.update(trader) == 1;
    }

    public boolean updateTraderAffiliation(String traderUUID, String affiliation) throws SQLException {
        Trader trader = traderDao.queryForId(traderUUID);
        if (trader != null) {
            trader.setAffiliation(affiliation);
            return traderDao.update(trader) == 1;
        }
        return false;
    }

    public boolean traderWithAffiliationExists(String affiliation) throws SQLException {
        return traderDao.queryBuilder().where().eq("affiliation", affiliation).countOf() > 0;
    }

    public Optional<Trader> getTraderByAffiliation(String affiliation) throws SQLException {
        List<Trader> traders = traderDao.queryBuilder().where().eq("affiliation", affiliation).query();
        return traders.isEmpty() ? Optional.empty() : Optional.of(traders.getFirst());
    }

    public boolean traderExists(String traderUUID) throws SQLException {
        return traderDao.idExists(traderUUID);
    }

    public Map<String, Trader> getTraders() {
        List<Trader> traders = Collections.emptyList();
        try {
            traders = traderDao.queryForAll();
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe("Could not get list of traders from database: " + e);
        }
        return traders.stream().collect(Collectors.toMap(Trader::getUUID, trader -> trader));
    }

    /*************************
     * Active Trade Missions *
     *************************/

    public void addActiveTradeMission(ActiveTradeMission mission) throws SQLException {
        activeTradeMissionDao.create(mission);
    }

    public void removeActiveTradeMission(int missionId) throws SQLException {
        activeTradeMissionDao.deleteById(missionId);
    }

    public void removeActiveTradeMission(ActiveTradeMission mission) throws SQLException {
        activeTradeMissionDao.delete(mission);
    }

    public boolean updateActiveTradeMission(ActiveTradeMission mission) throws SQLException {
        return activeTradeMissionDao.update(mission) == 1;
    }

    public Optional<ActiveTradeMission> getActiveTradeMissionById(int missionId) throws SQLException {
        return Optional.ofNullable(activeTradeMissionDao.queryForId(missionId));
    }
    public Optional<ActiveTradeMission> getActiveTradeMissionByPlayer(UUID playerUUID) throws SQLException {
        return Optional.ofNullable(activeTradeMissionDao.queryBuilder()
                .where().eq("playerUUID", playerUUID)
                .queryForFirst());
    }

    public List<ActiveTradeMission> getAllActiveTradeMissions() throws SQLException {
        return activeTradeMissionDao.queryForAll();
    }

    /*************************
     * Trade Mission Specs   *
     *************************/

     public void addTradeMissionSpec(TradeMissionSpec missionSpec) throws SQLException {
        tradeMissionSpecDao.create(missionSpec);
    }

    public void removeTradeMissionSpec(int missionSpecId) throws SQLException {
        tradeMissionSpecDao.deleteById(missionSpecId);
    }

    public void removeTradeMissionSpec(TradeMissionSpec missionSpec) throws SQLException {
        tradeMissionSpecDao.delete(missionSpec);
    }

    public boolean updateTradeMissionSpec(TradeMissionSpec missionSpec) throws SQLException {
        return tradeMissionSpecDao.update(missionSpec) == 1;
    }

    public Optional<TradeMissionSpec> getTradeMissionSpecById(int missionSpecId) throws SQLException {
        return Optional.ofNullable(tradeMissionSpecDao.queryForId(missionSpecId));
    }

    public List<TradeMissionSpec> getTradeMissionSpecsByStartTrader(Trader startTrader) throws SQLException {
        return tradeMissionSpecDao.queryBuilder()
                .where().eq("startTrader_id", startTrader.getUUID())
                .query();
    }

    public List<TradeMissionSpec> getAllTradeMissionSpecs() throws SQLException {
        return tradeMissionSpecDao.queryForAll();
    }
}
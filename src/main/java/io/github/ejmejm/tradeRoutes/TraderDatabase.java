package io.github.ejmejm.tradeRoutes;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcManager;
import io.github.ejmejm.tradeRoutes.dataclasses.Trader;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TraderDatabase {
    private static TraderDatabase INSTANCE;
    private static Connection connection;
    private static NpcManager npcManager;
    private static HashMap<String, Trader> traders;
    private static Plugin plugin;

    private TraderDatabase(String path, Plugin plugin) throws SQLException {
        // Connect to the database
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        // Create table if it doesn't exist, and create an index for affiliation
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS traders (
                uuid TEXT PRIMARY KEY,
                affiliation TEXT DEFAULT NULL
                );
            """);
            statement.execute("CREATE INDEX IF NOT EXISTS affiliation_index ON traders (affiliation)");
        }

        TraderDatabase.plugin = plugin;
        npcManager = FancyNpcsPlugin.get().getNpcManager();

        if (!syncDatabase()) {
            throw new SQLException("Failed to sync database with NPC registry.");
        }
    }

    public static void initialize(String path, Plugin plugin) throws SQLException {
        if (INSTANCE == null) {
            traders = new HashMap<>();
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

    /**
     * First syncs the contents of the database with that of the NpcRegistry.
     * The NpcRegistry is treated as the ground truth for which NPCs should exist.
     * NPCs not in the NpcRegistry are removed, and an error is thrown for any that should but do not exist
     * in the database (because then there is no way to determine affiliation).
     * The traders map is then populated with the remaining NPCs.
     */
    private boolean syncDatabase() {
        // First get UUIDs of all traders that should exist
        Map<String, Npc> npcMap = npcManager.getAllNpcs().stream()
                .filter(npc -> npc.getData().getName().startsWith(Trader.TRADER_NAME_PREFIX))
                .collect(Collectors.toMap(npc -> npc.getData().getId(), npc -> npc));

        Logger logger = plugin.getLogger();

        // Now loop through all traders in the database
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM traders")) {
                while (resultSet.next()) {
                    String uuid = resultSet.getString("uuid");
                    String affiliation = resultSet.getString("affiliation");

                    // If the NPC does not exist, remove it from the database
                    if (!npcMap.containsKey(uuid)) {
                        logger.warning("NPC with UUID "
                                + uuid + " does not exist in the NpcRegistry. Removing from database.");
                        removeTrader(uuid);
                    } else {
                        traders.put(uuid, new Trader(npcMap.get(uuid), affiliation));
                        npcMap.remove(uuid);
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to sync database with NPC registry: " + e);
            return false;
        }

        // Check if there are any NPCs in the NpcRegistry that are not in the database
        if (!npcMap.isEmpty()) {
            logger.warning("The following NPCs are in the NpcRegistry but not in the database:");
            npcMap.forEach((uuid, npc) -> logger.warning(uuid));
            logger.warning("Removing them from the NpcRegistry, which will remove them from the server.");

            for (Npc npc : npcMap.values()) {
                npc.removeForAll();
                npcManager.removeNpc(npc);
            }
        }

        return true;
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed())
            connection.close();
    }

    public void addTrader(Trader trader) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO traders (uuid, affiliation) VALUES (?, ?)")) {
            statement.setString(1, trader.getUUID());
            statement.setString(2, trader.getAffiliation());
            statement.executeUpdate();
        }
        traders.put(trader.getUUID(), trader);
    }


    public void removeTrader(String traderUUID) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM traders WHERE uuid = ?")) {
            statement.setString(1, traderUUID);
            statement.executeUpdate();
        }
        traders.remove(traderUUID);
    }

    public void removeTrader(Trader trader) throws SQLException { removeTrader(trader.getUUID()); }

    public boolean updateTrader(Trader trader) throws SQLException {
        // Add the trader if they do not exist
        if (!traderExists(trader.getUUID())) {
            addTrader(trader);
            return true;
        }

        try (PreparedStatement statement = connection.prepareStatement("UPDATE traders SET affiliation = ? WHERE uuid = ?")) {
            statement.setString(1, trader.getAffiliation());
            statement.setString(2, trader.getAffiliation());
            traders.put(trader.getUUID(), trader);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateTraderAffiliation(String traderUUID, String affiliation) throws SQLException {
        // Add the trader if they do not exist
        if (!traderExists(traderUUID)) {
            addTrader(new Trader(traderUUID, affiliation));
            return true;
        }

        try (PreparedStatement statement = connection.prepareStatement("UPDATE traders SET affiliation = ? WHERE uuid = ?")) {
            statement.setString(1, affiliation);
            statement.setString(2, traderUUID);
            traders.get(traderUUID).setAffiliation(affiliation);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean traderWithAffiliationExists(String affiliation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM traders WHERE affiliation = ?")) {
            statement.setString(1, affiliation);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<Trader> getTraderByAffiliation(String affiliation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM traders WHERE affiliation = ?")) {
            statement.setString(1, affiliation);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String uuid = resultSet.getString("uuid");
                    return Optional.of(traders.get(uuid));
                }
            }
        }
        return Optional.empty();
    }

    public boolean traderExists(String traderUUID) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM traders WHERE uuid = ?")) {
            statement.setString(1, traderUUID);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public HashMap<String, Trader> getTraders() {
        return traders;
    }
}

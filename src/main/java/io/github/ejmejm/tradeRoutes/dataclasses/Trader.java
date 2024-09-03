package io.github.ejmejm.tradeRoutes.dataclasses;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.utils.SkinFetcher;
import io.github.ejmejm.tradeRoutes.TradeRoutes;
import io.github.ejmejm.tradeRoutes.TraderDatabase;
import io.github.ejmejm.tradeRoutes.gui.TradeRouteMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@DatabaseTable(tableName = "traders")
public class Trader {
    public static String TRADER_NAME_PREFIX = "Trader-";
    private static final String SKIN_URL = "https://www.minecraftskins.net/therevenant/download";
    private static final String TRADER_NAME_COLOR = "<#8a44bd>";
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
        .create();

    @DatabaseField(id = true)
    private String uuid;

    @DatabaseField
    private String affiliation;

    @DatabaseField
    private String serializedMissionData;

    private int maxMissions = 20;
    private Duration missionDuration = Duration.ofMinutes(30); // Default 30 minutes
    private transient List<TraderMission> currentMissions;

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

    /**
     * Creates a new Trader NPC and spawns it in the world.
     *
     * @param spawnLoc The location to spawn the Trader NPC.
     * @param affiliation The affiliation of the Trader.
     * @param creator The player creating the Trader, can be null.
     * @return The newly created and spawned Trader.
     */
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

    /**
     * Handles the click event when a player interacts with the Trader NPC.
     * Checks if this is the player's destination for an active mission.
     * If so, it attempts to complete the mission.
     * If the player has an active mission that is not for this trader, it sends a message to the player.
     * If the player has no active missions, it displays the TradeRouteMenu for the player to select a new mission.
     *
     * @param player The player who clicked on the Trader.
     */
    private void onClick(Player player) {
        TraderDatabase db = TraderDatabase.getInstance();
        try {
            Optional<ActiveTradeMission> activeMission =
                    db.getActiveTradeMissionByPlayer(player.getUniqueId());
            if (activeMission.isEmpty()) {
                db.getTraderById(getUUID()).ifPresent(
                        trader -> new TradeRouteMenu(trader).displayTo(player));
            } else {
                if (activeMission.get().getMissionSpec().getEndTrader().getUUID().equals(this.getUUID())) {
                    activeMission.get().checkAndCompleteMission();
                } else {
                    player.sendMessage(Component.text(
                            "You already have an active mission that must be finished "
                            + "before interacting with other traders.",NamedTextColor.RED
                    ));
                }
            }
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe(
                    "Failed to check active trade missions: " + e.getMessage());
        }
    }

    /**
     * Connects the Trader to the NpcManager and sets up the click event.
     *
     * @return true if the connection was successful, false otherwise.
     */
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

    /**
     * Retrieves the list of trade missions for this Trader.
     * Initializes missions if they haven't been loaded, and replaces expired missions.
     *
     * @return A list of TradeMissionSpec objects representing the current trade missions.
     */
    public List<TradeMissionSpec> getTradeMissions() {
        List<TraderMission> currentMissions = getCurrentMissions();
        
        replaceExpiredMissions();

        return currentMissions.stream()
            .map(TraderMission::getMissionSpec)
            .sorted(Comparator.comparingDouble(spec -> getLocation().distance(spec.getEndTrader().getLocation())))
            .toList();
    }

    /**
     * Initializes the missions for this Trader.
     * If serialized mission data exists, it deserializes it.
     * Otherwise, it creates new missions for all other Traders.
     */
    public void initializeMissions() {
        if (serializedMissionData != null && !serializedMissionData.isEmpty()) {
            currentMissions = deserializeMissionData();
            for (TraderMission mission : currentMissions) {
                if (mission.getMissionSpec() == null) {
                    TradeRoutes.getInstance().getLogger().severe(
                            "Mission spec is null for mission " + mission.getMissionSpecId());
                    replaceMission(mission.getMissionSpecId(), true);
                }
            }
        } else {
            currentMissions = new ArrayList<>();
            Map<String, Trader> allTraders = TraderDatabase.getInstance().getTraders();
            allTraders.remove(this.getUUID());
            
            for (Trader endTrader : allTraders.values()) {
                addNewMission(endTrader, Instant.now());
            }
            updateSerializedMissionData();
        }
    }

    /**
     * Replaces expired missions with new ones.
     */
    private void replaceExpiredMissions() {
        Instant now = Instant.now();
        boolean missionsUpdated = false;

        List<TraderMission> expiredMissions = getCurrentMissions().stream()
            .filter(mission -> mission.getExpirationTime().isBefore(now))
            .toList();

        for (TraderMission expiredMission : expiredMissions) {
            Trader endTrader = expiredMission.getMissionSpec().getEndTrader();
            try {
                getCurrentMissions().remove(expiredMission);
                TraderDatabase.getInstance().removeTradeMissionSpec(expiredMission.getMissionSpec());
                missionsUpdated = true;

                if (TraderDatabase.getInstance().traderExists(endTrader.getUUID())) {
                    addNewMission(endTrader, expiredMission.getExpirationTime());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        if (missionsUpdated)
            updateSerializedMissionData();
    }
    
    /**
     * Replaces a single mission with a new one for the same end trader.
     * If the mission spec is null (db retrieval problem), then it is just deleted.
     *
     * @param oldMissionId The mission spec to be replaced.
     * @param deleteFromDatabase Whether to delete the old mission from the database.
     */
    public void replaceMission(int oldMissionId, boolean deleteFromDatabase) {
        // Find the mission that contains the old mission spec
        TraderMission missionToRemove = getCurrentMissions().stream()
            .filter(mission -> mission.getMissionSpecId() == oldMissionId)
            .findFirst()
            .orElse(null);

        if (missionToRemove == null)
            return;
        getCurrentMissions().remove(missionToRemove);

        try {
            TradeMissionSpec oldMissionSpec = missionToRemove.getMissionSpec();
            if (oldMissionSpec != null) {
                if (deleteFromDatabase)
                    TraderDatabase.getInstance().removeTradeMissionSpec(oldMissionSpec);

                Trader endTrader = oldMissionSpec.getEndTrader();
                if (TraderDatabase.getInstance().traderExists(endTrader.getUUID())) {
                    addNewMission(endTrader, Instant.now());
                }
            } else {
                TradeRoutes.getInstance().getLogger().severe("Mission spec is null, so it could not be replaced!");
            }
            updateSerializedMissionData();
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe("Failed to replace mission: " + e.getMessage());
        }
    }

    public void replaceMission(TradeMissionSpec oldMission, boolean deleteFromDatabase) {
        replaceMission(oldMission.getId(), deleteFromDatabase);
    }

    /**
     * Removes a single mission without replacing it.
     *
     * @param missionId The ID of the mission to be removed.
     * @param deleteFromDatabase Whether to delete the mission from the database.
     */
    public void removeMission(int missionId, boolean deleteFromDatabase) {
        // Find the mission that contains the mission spec to remove
        TraderMission missionToDelete = getCurrentMissions().stream()
            .filter(mission -> mission.getMissionSpecId() == missionId)
            .findFirst()
            .orElse(null);

        if (missionToDelete == null)
            return;
        getCurrentMissions().remove(missionToDelete);

        try {
            TradeMissionSpec missionSpec = missionToDelete.getMissionSpec();
            if (missionSpec != null && deleteFromDatabase)
                TraderDatabase.getInstance().removeTradeMissionSpec(missionSpec);

            updateSerializedMissionData();
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe("Failed to remove mission: " + e.getMessage());
        }
    }

    /**
     * Removes a single mission without replacing it.
     *
     * @param missionToRemove The mission spec to be removed.
     * @param deleteFromDatabase Whether to delete the mission from the database.
     */
    public void removeMission(TradeMissionSpec missionToRemove, boolean deleteFromDatabase) {
        removeMission(missionToRemove.getId(), deleteFromDatabase);
    }

    /**
     * Adds a new mission to the current missions list.
     *
     * @param endTrader The end Trader for the new mission.
     * @param baseTime The base time for the new mission's start time.
     */
    private void addNewMission(Trader endTrader, Instant baseTime) {
        TradeMissionSpec newMissionSpec = TradeMissionSpec.createRandomMission(this, endTrader);
        try {
            TraderDatabase.getInstance().addTradeMissionSpec(newMissionSpec);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Instant expirationTime = baseTime.plus(missionDuration);

        List<TraderMission> currentMissions = getCurrentMissions();

        currentMissions.add(new TraderMission(newMissionSpec, baseTime, expirationTime));

        // Ensure we don't exceed maxMissions
        if (currentMissions.size() > maxMissions) {
            currentMissions.sort(Comparator.comparing(TraderMission::getStartTime));
            this.currentMissions = currentMissions.subList(0, maxMissions);
        }
    }

    /**
     * Updates the serialized mission data in the database.
     */
    private void updateSerializedMissionData() {
        serializedMissionData = gson.toJson(getCurrentMissions());
        try {
            TraderDatabase.getInstance().updateTrader(this);
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe("Failed to update trader in database: " + e.getMessage());
        }
    }

    /**
     * Deserializes the mission data from JSON to a List of TraderMission objects.
     *
     * @return A List of TraderMission objects.
     */
    private List<TraderMission> deserializeMissionData() {
        return gson.fromJson(serializedMissionData, new TypeToken<List<TraderMission>>(){}.getType());
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

    public int getMaxMissions() {
        return maxMissions;
    }

    public void setMaxMissions(int maxMissions) {
        this.maxMissions = maxMissions;
    }

    private List<TraderMission> getCurrentMissions() {
        if (currentMissions == null)
            initializeMissions();
        return currentMissions;
    }


    private static class TraderMission {
        private final int missionSpecId;
        private final Instant startTime;
        private Instant expirationTime;
        private transient TradeMissionSpec missionSpec;

        public TraderMission(TradeMissionSpec missionSpec, Instant startTime, Instant expirationTime) {
            this.missionSpec = missionSpec;
            this.missionSpecId = missionSpec.getId();
            this.startTime = startTime;
            this.expirationTime = expirationTime;
        }

        public int getMissionSpecId() {
            return missionSpecId;
        }

        public TradeMissionSpec getMissionSpec() {
            if (missionSpec == null) {
                try {
                    missionSpec = TraderDatabase.getInstance().getTradeMissionSpecById(missionSpecId).orElse(null);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            return missionSpec;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getExpirationTime() {
            return expirationTime;
        }
    }


    private static class InstantTypeAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            return Instant.parse(in.nextString());
        }
    }

}

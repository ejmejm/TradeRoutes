package io.github.ejmejm.tradeRoutes.dataclasses;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.github.ejmejm.tradeRoutes.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Camel;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.SQLException;
import java.util.*;

@DatabaseTable(tableName = "active_trade_missions")
public class ActiveTradeMission {

    private static final int MISSION_DURATION = 72 * 60 * 60 * 1000; // 72 hours in milliseconds
    private static final float CAMEL_HEALTH = 80.0f;

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private TradeMissionSpec missionSpec;

    @DatabaseField(unique = true, index = true)
    private UUID playerUUID;

    @DatabaseField
    private String playerName;

    @DatabaseField
    private UUID caravanUUID;

    @DatabaseField
    private Date startTime;

    @DatabaseField
    private Date expiryTime;

    // No-args constructor required by ORMLite
    public ActiveTradeMission() {}

    public ActiveTradeMission(
            TradeMissionSpec missionSpec,
            UUID playerUUID,
            UUID caravanUUID,
            Date startTime,
            Date expiryTime) {
        this.missionSpec = missionSpec;
        this.playerUUID = playerUUID;
        this.caravanUUID = caravanUUID;
        this.startTime = startTime;
        this.expiryTime = expiryTime;
        Player player = Bukkit.getPlayer(playerUUID);
        this.playerName = player == null ? null : player.getName();
    }

    public static ActiveTradeMission initiateMission(Player player, TradeMissionSpec missionSpec) {
        // Remove required items from player's inventory
        for (ItemStack requiredItem : missionSpec.getRequiredItems()) {
            player.getInventory().removeItem(requiredItem);
        }
        // Spawn and setup camel
        Location spawnLocation = player.getLocation();
        Camel camel = (Camel) player.getWorld().spawnEntity(spawnLocation, EntityType.CAMEL);
        camel.customName(Component.text("Trade Caravan"));
        float caravan_health = TradeConfig.getFloat("caravan_health", missionSpec.getStartTrader().getLevel());
        Objects.requireNonNull(camel.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(caravan_health);
        camel.setHealth(caravan_health);
        camel.setRemoveWhenFarAway(false);
        PotionEffect glowEffect = new PotionEffect(
            PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION,
                0, false, false, false);
        camel.addPotionEffect(glowEffect);
        camel.setLeashHolder(player);

        // Store required items in persistent data container
        NamespacedKey dropsKey = new NamespacedKey(TradeRoutes.getInstance(), Constants.DEATH_ITEMS_META_KEY);
        PersistentDataContainer container = camel.getPersistentDataContainer();
        container.set(
                dropsKey, PersistentDataType.STRING, ItemUtils.serializeItemStacks(missionSpec.getRequiredItems()));

        // Create and save new ActiveTradeMission
        Date startTime = new Date();
        Date expiryTime = new Date(startTime.getTime() + MISSION_DURATION);

        ActiveTradeMission mission = new ActiveTradeMission(
                missionSpec, player.getUniqueId(), camel.getUniqueId(), startTime, expiryTime);

        missionSpec.setTaken(true);

        try {
            TraderDatabase db = TraderDatabase.getInstance();
            db.updateTradeMissionSpec(missionSpec);
            db.addActiveTradeMission(mission);

            // Store mission ID in the camel's persistent data
            NamespacedKey key = new NamespacedKey(TradeRoutes.getInstance(), Constants.CARAVAN_MISSION_META_KEY);
            camel.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, mission.getId());
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe(
                    "Failed to add active trade mission to database: " + e.getMessage());
            return null;
        }

        return mission;
    }

    public boolean completeMission() {
        // Remove mission from database
        try {
            TraderDatabase db = TraderDatabase.getInstance();
            if (TradeConfig.getBool("replace_mission_on_completion")) {
                replaceOriginalMission();
            } else if (!startStillHasMission()) {
                db.removeTradeMissionSpec(missionSpec);
            }
            db.removeActiveTradeMission(this);
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe(
                    "Failed to remove completed mission from database: " + e.getMessage());
            return false;
        }

        // Remove camel
        Camel camel = (Camel) Bukkit.getEntity(caravanUUID);
        if (camel != null) {
            camel.remove();
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            // Give rewards to the player
            for (ItemStack reward : missionSpec.getRewards()) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItem(player.getLocation(), leftover.get(0));
                }
            }

            // Send success message
            player.sendMessage(Component.text(
                    "Trade mission completed successfully! You've received your rewards.", NamedTextColor.GREEN));
        }

        return true;
    }

    public void failMission(Component failureMessage) {
        // Remove mission from database
        try {
            TraderDatabase db = TraderDatabase.getInstance();
            if (TradeConfig.getBool("replace_mission_on_failure")) {
                replaceOriginalMission();
            } else if (!startStillHasMission()) {
                db.removeTradeMissionSpec(missionSpec);
            }
            db.removeActiveTradeMission(this);
        } catch (SQLException e) {
            TradeRoutes.getInstance().getLogger().severe(
                    "Failed to remove failed mission from database: " + e.getMessage());
            return;
        }

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            // Send failure message
            player.sendMessage(Objects.requireNonNullElse(
                    failureMessage,
                    Component.text("Trade mission failed. Better luck next time!", NamedTextColor.RED)
            ));
        }

    }

    public void checkAndCompleteMission() {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null)
            return;

        Trader destinationTrader = getMissionSpec().getEndTrader();
        Location traderLocation = destinationTrader.getLocation();
        Location playerLocation = player.getLocation();
        Camel camel = (Camel) Bukkit.getEntity(getCaravanUUID());

        boolean playerClose = playerLocation.distance(traderLocation) <= 30;
        boolean camelClose = camel != null && camel.getLocation().distance(traderLocation) <= 30;

        if (playerClose && camelClose) {
            if (!completeMission()) {
                player.sendMessage(Component.text(
                        "There was an error completing the mission. Please contact an admin.", NamedTextColor.RED));
            }
        } else {
            Component message = Component.text("Cannot complete the mission: ", NamedTextColor.RED);
            List<Component> problems = new ArrayList<>();
            
            if (!playerClose) {
                problems.add(Component.text(
                        "- You need to be within 30 blocks of the destination trader.", NamedTextColor.RED));
            }
            if (!camelClose) {
                problems.add(Component.text(
                        "- Your caravan needs to be within 30 blocks of the destination trader.", NamedTextColor.RED));
            }
            
            for (Component problem : problems) {
                message = message.append(Component.newline()).append(problem);
            }
            
            player.sendMessage(message);
        }
    }

    private boolean startStillHasMission() {
        return missionSpec.getStartTrader().getTradeMissions().stream()
            .anyMatch(spec -> spec.getId() == missionSpec.getId());
    }

    /*
     * Replaces the mission at the trader it came from if it is still there.
     */
    private void replaceOriginalMission() {
        missionSpec.getStartTrader().replaceMission(missionSpec, true);
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public TradeMissionSpec getMissionSpec() {
        return missionSpec;
    }

    public void setMissionSpec(TradeMissionSpec missionSpec) {
        this.missionSpec = missionSpec;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Date expiryTime) {
        this.expiryTime = expiryTime;
    }

    public UUID getCaravanUUID() {
        return caravanUUID;
    }

    public void setCaravanUUID(UUID caravanUUID) {
        this.caravanUUID = caravanUUID;
    }
}
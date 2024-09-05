package io.github.ejmejm.tradeRoutes;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import io.github.ejmejm.tradeRoutes.events.CaravanDeathListener;
import io.github.ejmejm.tradeRoutes.events.MenuListener;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public final class TradeRoutes extends JavaPlugin {

    private static final List<String> SOFT_DEPENDENCIES = List.of("Towny");
    private static BukkitTask initDbTask;

    private void initializeDatabase() {
        try {
            getLogger().info("Attempting to connect to database.");
            if (!getDataFolder().exists()) {
                boolean created = getDataFolder().mkdirs();
                if (!created) {
                    getLogger().severe("Failed to create data folder. Disabling plugin.");
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            }
            TraderDatabase.initialize(getDataFolder().getAbsolutePath() + "traders.db", this);
            getLogger().info("Successfully connected to database.");
        } catch (SQLException | IllegalStateException e) {
            getLogger().severe("Failed to connect to database: " + e);
            getServer().getPluginManager().disablePlugin(this);
            e.printStackTrace();
        }
    }

    private void scheduleDatabaseInitTask() {
        // Schedule a task to check when NPCs are fully loaded
        initDbTask = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            private int previousNpcCount = 0;
            private int unchangedTickCount = 0;

            @Override
            public void run() {
                int currentNpcCount = FancyNpcsPlugin.get().getNpcManager().getAllNpcs().size();

                // Check if the number of NPCs has changed
                if (currentNpcCount > previousNpcCount) {
                    unchangedTickCount = 0; // Reset the tick count
                    previousNpcCount = currentNpcCount;
                } else {
                    unchangedTickCount++;
                }

                // Initialize database if NPC count is still 0 after 30 seconds or if the count stabilizes for 1 second
                if ((currentNpcCount > 0 && unchangedTickCount >= 2) || unchangedTickCount >= 60) {
                    getServer().getScheduler().cancelTask(initDbTask.getTaskId());
                    initializeDatabase();

                    // Register a task to frequently check to make sure all missions are okay
                    MissionManager.registerMissionCheckTask(getInstance());
                }

            }
        }, 20L * 5, 10L); // Run every 10 ticks (0.5 seconds)
    }

    @Override
    public void onEnable() {
        // Register which soft dependencies are enabled
        PluginChecker.initialize(SOFT_DEPENDENCIES);

        // Load configs
        try {
            ItemValues.initialize();
            TradeConfig.initialize();
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Failed to initialize configs: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    
        // Prepare NPCs
        Plugin fancyNpcs = getServer().getPluginManager().getPlugin("FancyNpcs");
        if (fancyNpcs == null || !fancyNpcs.isEnabled()) {
            getLogger().severe("FancyNPCs plugin did not load! Disabling Trade Routes plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        scheduleDatabaseInitTask();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new CaravanDeathListener(), this);

        // Register commands
        getCommand("traderoutes").setExecutor(new CommandManager());
    }

    @Override
    public void onDisable() {
        // Close the database connection
        try {
            TraderDatabase.getInstance().closeConnection();
            getLogger().info("Database connection closed successfully.");
        } catch (Exception e) {
            getLogger().warning("Failed to close database connection: " + e);
        }
    }

    public static TradeRoutes getInstance() {
        return getPlugin(TradeRoutes.class);
    }
}

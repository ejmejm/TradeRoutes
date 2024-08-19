package io.github.ejmejm.tradeRoutes;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class TradeRoutes extends JavaPlugin {

    private void initializeDatabase() {
        try {
            getLogger().info("Attempting to connect to database.");
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            TraderDatabase.initialize(getDataFolder().getAbsolutePath() + "traders.db", this);
            getLogger().info("Successfully connected to database.");
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to database: " + e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onEnable() {
        FancyNpcsPlugin.get().getNpcManager().loadNpcs();
        initializeDatabase();
        getCommand("traderoutes").setExecutor(new CommandManager());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

package io.github.ejmejm.tradeRoutes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginChecker {

    // The Singleton instance
    private static PluginChecker instance;

    // Map to store the plugin names and their enabled status
    private final Map<String, Boolean> pluginStatusMap = new HashMap<>();

    // Private constructor to prevent instantiation
    private PluginChecker(List<String> pluginNames) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        for (String pluginName : pluginNames) {
            Plugin plugin = pluginManager.getPlugin(pluginName);
            boolean isEnabled = plugin != null && plugin.isEnabled();
            pluginStatusMap.put(pluginName, isEnabled);
        }
    }

    // Method to initialize the Singleton instance
    public static synchronized PluginChecker initialize(List<String> pluginNames) {
        if (instance != null) {
            throw new IllegalStateException("PluginChecker has already been initialized!");
        }
        instance = new PluginChecker(pluginNames);
        return instance;
    }

    // Method to get the already initialized Singleton instance
    public static PluginChecker getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PluginChecker is not initialized! Call initialize(List<String> pluginNames) first.");
        }
        return instance;
    }

    // Method to check if a specific plugin is enabled
    public boolean isPluginEnabled(String pluginName) {
        return pluginStatusMap.getOrDefault(pluginName, false);
    }
}

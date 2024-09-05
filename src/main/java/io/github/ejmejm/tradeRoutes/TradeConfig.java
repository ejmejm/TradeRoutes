package io.github.ejmejm.tradeRoutes;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TradeConfig {
    private static YamlConfiguration config;
    private static Map<Integer, Map<String, Object>> levelConfigs = new HashMap<>();

    public static void initialize() throws IOException {
        TradeRoutes plugin = TradeRoutes.getInstance();
        File file = new File(plugin.getDataFolder(), "config.yml");
        
        if (!file.exists()) {
            plugin.getLogger().warning("config.yml not found, creating default file!");
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        // Load level-specific configurations
        if (config.contains("levels")) {
            for (String levelKey : config.getConfigurationSection("levels").getKeys(false)) {
                int level = Integer.parseInt(levelKey);
                levelConfigs.put(level, config.getConfigurationSection("levels." + levelKey).getValues(false));
            }
        }
    }
    
    private static void throwKeyNotFoundError(String key) {
        throw new IllegalArgumentException("Config key not found: " + key);
    }

    public static int getInt(String key) {
        if (!config.contains(key)) {
            throwKeyNotFoundError(key);
        }
        return config.getInt(key);
    }

    public static int getInt(String key, int level) {
        if (!levelConfigs.containsKey(level) || !levelConfigs.get(level).containsKey(key))
            throwKeyNotFoundError(key);
        return (int) levelConfigs.get(level).get(key);
    }

    public static float getFloat(String key) {
        if (!config.contains(key))
            throwKeyNotFoundError(key);
        return (float) config.getDouble(key);
    }

    public static float getFloat(String key, int level) {
        if (!levelConfigs.containsKey(level) || !levelConfigs.get(level).containsKey(key))
            throwKeyNotFoundError(key);
        return ((Number) levelConfigs.get(level).get(key)).floatValue();
    }

    public static boolean getBool(String key) {
        if (!config.contains(key))
            throwKeyNotFoundError(key);
        return config.getBoolean(key);
    }

    public static boolean getBool(String key, int level) {
        if (!levelConfigs.containsKey(level) || !levelConfigs.get(level).containsKey(key))
            throwKeyNotFoundError(key);
        return (boolean) levelConfigs.get(level).get(key);
    }
}
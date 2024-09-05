package io.github.ejmejm.tradeRoutes;

import io.github.ejmejm.tradeRoutes.dataclasses.TradeItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class TradeConfig {
    private static YamlConfiguration config;
    private static Map<Integer, Map<String, Object>> levelConfigs = new HashMap<>();
    private static Map<Integer, Map<TradeItem, Double>> rewardPools = new HashMap<>();
    private static Map<Integer, Map<TradeItem, Double>> requirementPools = new HashMap<>();

    public static void initialize() throws IOException, InvalidConfigurationException {
        TradeRoutes plugin = TradeRoutes.getInstance();
        File file = new File(plugin.getDataFolder(), "config.yml");
        
        if (!file.exists()) {
            plugin.getLogger().warning("config.yml not found, creating default file!");
            plugin.saveResource("config.yml", false);
        }

        config = new YamlConfiguration();
        config.options().parseComments(true);
        config.load(file);

        // Load level-specific configurations
        if (config.contains("levels")) {
            for (String levelKey : config.getConfigurationSection("levels").getKeys(false)) {
                int level = Integer.parseInt(levelKey);
                levelConfigs.put(level, config.getConfigurationSection("levels." + levelKey).getValues(false));
            }
        }

        // Load the reward and requirement pools
        rewardPools = loadItemPool("mission_reward_pool");
        requirementPools = loadItemPool("mission_requirement_pool");
    }

    private static Map<Integer, Map<TradeItem, Double>> loadItemPool(String poolType) {
        Map<Integer, Map<TradeItem, Double>> levelPools = new HashMap<>();
        Logger logger = TradeRoutes.getInstance().getLogger();

        // Loop through each level and load the item pool for that level
        for (String levelKey : config.getConfigurationSection("levels").getKeys(false)) {
            int level = Integer.parseInt(levelKey);
            ConfigurationSection levelSection = config.getConfigurationSection("levels." + levelKey);
            ConfigurationSection poolSelection = levelSection.getConfigurationSection(poolType);
            if (poolSelection == null) {
                throw new IllegalStateException("Pool '" + poolType + "' not found for level " + level);
            }

            Map<TradeItem, Double> itemPool = new HashMap<>();
            double totalWeight = 0;

            // Loop through each category in the pool
            for (String categoryName : poolSelection.getKeys(false)) {
                ConfigurationSection categorySection = poolSelection.getConfigurationSection(categoryName);

                // Set the default values for the category
                double categoryWeight = categorySection.getDouble("weight", 1);
                int defaultMinAmount = categorySection.getInt("min_amount", 1);
                int defaultMaxAmount = categorySection.getInt("max_amount", 1);

                // Get the items for the category, and skip if no items are defined, or if the weight is 0
                List<Map<?, ?>> itemsList = categorySection.getMapList("items");
                if (itemsList.isEmpty() || categoryWeight <= 0) continue;

                double categoryTotalWeight = 0;
                List<TradeItem> categoryItems = new ArrayList<>();
                List<Double> categoryItemWeights = new ArrayList<>();

                // Loop through each item in the category
                for (Map<?, ?> itemMap : itemsList) {
                    if (!itemMap.containsKey("item"))
                        continue;

                    String itemName = (String) itemMap.get("item");
                    Material material = Material.getMaterial(itemName);

                    if (material == null) {
                        logger.warning(
                                "Invalid item type in " + poolType + " pool config: " + itemName);
                        continue;
                    }
                    
                    // Get the stats for the item
                    double itemWeight = itemMap.containsKey("weight") ? ((Number) itemMap.get("weight")).doubleValue() : 1;
                    int minAmount = itemMap.containsKey("min_amount") ? ((Number) itemMap.get("min_amount")).intValue() : defaultMinAmount;
                    int maxAmount = itemMap.containsKey("max_amount") ? ((Number) itemMap.get("max_amount")).intValue() : defaultMaxAmount;
                    Optional<Float> valueOpt = ItemValues.getMaterialValue(material);

                    if (valueOpt.isEmpty()) {
                        logger.warning("Material '" + material + "' in " + poolType
                                + " pool config is not defined in item_values.yml");
                        logger.warning("This item will not be available for missions");
                        continue;
                    }

                    TradeItem tradeItem = new TradeItem(
                        new ItemStack(material),
                        categoryName,
                        minAmount,
                        maxAmount,
                        valueOpt.get()
                    );

                    categoryItems.add(tradeItem);
                    categoryItemWeights.add(itemWeight);
                    categoryTotalWeight += itemWeight;
                }

                // Calculate the probability of each item in the category
                for (int i = 0; i < categoryItems.size(); i++) {
                    double itemProbability = (categoryItemWeights.get(i) / categoryTotalWeight) * categoryWeight;
                    itemPool.put(categoryItems.get(i), itemProbability);
                    totalWeight += itemProbability;
                }
            }

            // Normalize probabilities
            for (Map.Entry<TradeItem, Double> entry : itemPool.entrySet()) {
                entry.setValue(entry.getValue() / totalWeight);
            }

            levelPools.put(level, itemPool);
        }

        return levelPools;
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

    public static Map<TradeItem, Double> getRewardPool(int level) {
        if (!rewardPools.containsKey(level))
            throw new IllegalArgumentException("Reward pool not found for level: " + level);
        return rewardPools.get(level);
    }

    public static Map<TradeItem, Double> getRequirementPool(int level) {
        if (!requirementPools.containsKey(level))
            throw new IllegalArgumentException("Requirement pool not found for level: " + level);
        return requirementPools.get(level);
    }
}
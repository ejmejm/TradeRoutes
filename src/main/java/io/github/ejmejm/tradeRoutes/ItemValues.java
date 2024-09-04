package io.github.ejmejm.tradeRoutes;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ItemValues {

    private static final Map<Material, Float> itemValues = new HashMap<>();

    public static void initialize() throws IOException, InvalidConfigurationException {
        TradeRoutes plugin = TradeRoutes.getInstance();

        File file = new File(plugin.getDataFolder(), "item_values.yml");
        
        if (!file.exists()) {
            plugin.getLogger().warning("item_values.yml not found, creating default file, which needs to be edited!");
            plugin.saveResource("item_values.yml", false);
        }

        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        config.load(file);

        for (String key : config.getKeys(false)) {
            Material material = Material.getMaterial(key);
            if (material == null) {
                plugin.getLogger().warning("Invalid material in item_values.yml: " + key);
            } else {
                int value = config.getInt(key);
                itemValues.put(material, (float) value);
            }
        }

        plugin.getLogger().info("Loaded " + itemValues.size() + " item values.");
    }

    public static Optional<Float> getMaterialValue(Material material) {
        return Optional.ofNullable(itemValues.get(material));
    }

    public static Optional<Float> getItemStackValue(ItemStack itemStack) {
        Optional<Float> matValue = getMaterialValue(itemStack.getType());
        if (matValue.isPresent())
            return Optional.of(matValue.get() * itemStack.getAmount());
        return Optional.empty();
    }

    public static boolean hasValueForMaterial(Material material) {
        return itemValues.containsKey(material);
    }
}
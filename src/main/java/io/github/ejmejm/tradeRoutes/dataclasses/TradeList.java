package io.github.ejmejm.tradeRoutes.dataclasses;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class TradeList {
    public static final List<TradeItem> REQUIRED_ITEMS = Arrays.asList(
            new TradeItem(new ItemStack(Material.DIAMOND), ItemCategory.VALUABLES, 1, 5, 100.0f),
            new TradeItem(new ItemStack(Material.IRON_INGOT), ItemCategory.VALUABLES, 10, 64, 10.0f),
            new TradeItem(new ItemStack(Material.OAK_LOG), ItemCategory.BUILDING_BLOCKS, 16, 64, 2.0f),
            new TradeItem(new ItemStack(Material.GOLDEN_SWORD), ItemCategory.EQUIPMENT, 1, 1, 50.0f),
            new TradeItem(new ItemStack(Material.COBBLESTONE), ItemCategory.BUILDING_BLOCKS, 32, 128, 1.0f),
            new TradeItem(new ItemStack(Material.GOLD_INGOT), ItemCategory.VALUABLES, 5, 32, 20.0f),
            new TradeItem(new ItemStack(Material.IRON_SWORD), ItemCategory.EQUIPMENT, 1, 1, 30.0f),
            new TradeItem(new ItemStack(Material.BRICK), ItemCategory.BUILDING_BLOCKS, 16, 64, 3.0f),
            new TradeItem(new ItemStack(Material.EMERALD), ItemCategory.VALUABLES, 1, 10, 50.0f),
            new TradeItem(new ItemStack(Material.DIAMOND_PICKAXE), ItemCategory.EQUIPMENT, 1, 1, 150.0f)
    );

    public static final List<TradeItem> REWARDS = Arrays.asList(
            new TradeItem(new ItemStack(Material.EMERALD), ItemCategory.VALUABLES, 1, 10, 50.0f),
            new TradeItem(new ItemStack(Material.DIAMOND_SWORD), ItemCategory.EQUIPMENT, 1, 1, 150.0f),
            new TradeItem(new ItemStack(Material.STONE_BRICKS), ItemCategory.BUILDING_BLOCKS, 32, 64, 1.0f),
            new TradeItem(new ItemStack(Material.GOLDEN_APPLE), ItemCategory.VALUABLES, 1, 5, 100.0f),
            new TradeItem(new ItemStack(Material.IRON_HELMET), ItemCategory.EQUIPMENT, 1, 1, 40.0f),
            new TradeItem(new ItemStack(Material.OBSIDIAN), ItemCategory.BUILDING_BLOCKS, 8, 32, 10.0f),
            new TradeItem(new ItemStack(Material.NETHERITE_INGOT), ItemCategory.VALUABLES, 1, 4, 200.0f),
            new TradeItem(new ItemStack(Material.DIAMOND_CHESTPLATE), ItemCategory.EQUIPMENT, 1, 1, 300.0f),
            new TradeItem(new ItemStack(Material.QUARTZ_BLOCK), ItemCategory.BUILDING_BLOCKS, 16, 64, 5.0f),
            new TradeItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), ItemCategory.VALUABLES, 1, 2, 500.0f)
    );
}
package io.github.ejmejm.tradeRoutes.dataclasses;

import org.bukkit.inventory.ItemStack;

public class TradeItem {
    private ItemStack item;
    private ItemCategory category;
    private int minQuantity;
    private int maxQuantity;
    private float valuePerItem;

    public TradeItem(ItemStack item, ItemCategory category, int minQuantity, int maxQuantity, float valuePerItem) {
        this.item = item;
        this.category = category;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.valuePerItem = valuePerItem;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public float getValuePerItem() {
        return valuePerItem;
    }
}
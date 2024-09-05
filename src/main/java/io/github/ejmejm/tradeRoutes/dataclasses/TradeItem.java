package io.github.ejmejm.tradeRoutes.dataclasses;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class TradeItem {
    private ItemStack item;
    private String category;
    private int minQuantity;
    private int maxQuantity;
    private double valuePerItem;

    public TradeItem(ItemStack item, String category, int minQuantity, int maxQuantity, float valuePerItem) {
        this.item = item;
        this.category = category;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.valuePerItem = valuePerItem;
    }
    public ItemStack getItem() {
        return item.clone();
    }

    public String getCategory() {
        return category;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public double getValuePerItem() {
        return valuePerItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeItem tradeItem = (TradeItem) o;
        return minQuantity == tradeItem.minQuantity &&
               maxQuantity == tradeItem.maxQuantity &&
               Double.compare(tradeItem.valuePerItem, valuePerItem) == 0 &&
               item.isSimilar(tradeItem.item) &&
               Objects.equals(category, tradeItem.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item.getType(), category, minQuantity, maxQuantity, valuePerItem);
    }
}
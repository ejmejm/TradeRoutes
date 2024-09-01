package io.github.ejmejm.tradeRoutes.dataclasses;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TradeMission {
    private Trader startTrader;
    private Trader endTrader;
    private List<ItemStack> requiredItems;
    private List<ItemStack> rewards;

    public TradeMission(List<ItemStack> requiredItems, Trader startTrader, Trader endTrader, List<ItemStack> rewards) {
        this.requiredItems = requiredItems;
        this.startTrader = startTrader;
        this.endTrader = endTrader;
        this.rewards = rewards;
    }

    public static TradeMission createRandomMission(Trader startTrader, Trader endTrader) {
        List<ItemStack> requiredItems = generateTradeItems(100.0f, 500.0f, 2, 4, TradeList.REQUIRED_ITEMS);
        List<ItemStack> rewards = generateTradeItems(100.0f, 1000.0f, 2, 4, TradeList.REWARDS);
        return new TradeMission(requiredItems, startTrader, endTrader, rewards);
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    public Trader getStartTrader() {
        return startTrader;
    }

    public Trader getEndTrader() {
        return endTrader;
    }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    public double getRouteDistance() {
        return startTrader.getLocation().distance(endTrader.getLocation());
    }

    public static TradeMission createFromStock(
            List<ItemStack> stockRequiredItems,
            List<ItemStack> stockRewards,
            Trader startTrader,
            Trader endTrader) {
        return new TradeMission(stockRequiredItems, startTrader, endTrader, stockRewards);
    }

    public static List<ItemStack> generateTradeItems(
            float minValue,
            float maxValue,
            int minUniqueItems,
            int maxUniqueItems,
            List<TradeItem> stockList) {
        Random random = new Random();
        List<ItemStack> generatedItems = new ArrayList<>();
        HashMap<ItemStack, Float> itemStackValues = new HashMap<>();
        float totalValue = 0;
        int uniqueItemsCount = random.nextInt(maxUniqueItems - minUniqueItems + 1) + minUniqueItems;
        int maxRetries = 5;
        int retryCount = 0;

        while (generatedItems.size() < uniqueItemsCount && totalValue < maxValue) {
            TradeItem tradeItem = stockList.get(random.nextInt(stockList.size()));
            float minStackVal = tradeItem.getValuePerItem() * tradeItem.getMinQuantity();
            if (totalValue + minStackVal <= maxValue) {
                int maxStackQuantity = (int) ((maxValue - totalValue) / tradeItem.getValuePerItem());
                maxStackQuantity = Math.min(maxStackQuantity, tradeItem.getMaxQuantity());
                int quantity = random.nextInt(
                        maxStackQuantity - tradeItem.getMinQuantity() + 1) + tradeItem.getMinQuantity();

                float stackValue = tradeItem.getValuePerItem() * quantity;

                totalValue += stackValue;
                ItemStack itemStack = tradeItem.getItem().clone();
                itemStack.setAmount(quantity);

                generatedItems.add(itemStack);
                itemStackValues.put(itemStack, stackValue);
            }

            // If we reach the maximum number of unique items but totalValue is still below minValue
            if (generatedItems.size() >= uniqueItemsCount && totalValue < minValue && retryCount < maxRetries) {
                // Find and remove the least valuable item stack
                ItemStack leastValuable = generatedItems.stream()
                        .min(Comparator.comparing(itemStackValues::get))
                        .orElse(null);

                if (leastValuable != null) {
                    totalValue -= itemStackValues.get(leastValuable);
                    generatedItems.remove(leastValuable);
                    itemStackValues.remove(leastValuable);
                }
            }

            // Try to add another item to reach minValue
            retryCount++;

            // If max retries exceeded, break out of the loop
            if (retryCount >= maxRetries) {
                break;
            }
        }

        // Final check: if totalValue is still below minValue, just return what was generated
        return generatedItems;
    }

}
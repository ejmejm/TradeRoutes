package io.github.ejmejm.tradeRoutes.dataclasses;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.*;

@DatabaseTable(tableName = "trade_mission_specs")
public class TradeMissionSpec {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Trader startTrader;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Trader endTrader;

    @DatabaseField
    private String requiredItemsSerialized;

    @DatabaseField
    private String rewardsSerialized;

    // Transient fields for quick access
    private transient List<ItemStack> requiredItems;
    private transient List<ItemStack> rewards;

    public TradeMissionSpec() {}

    public TradeMissionSpec(List<ItemStack> requiredItems, Trader startTrader, Trader endTrader, List<ItemStack> rewards) {
        this.requiredItemsSerialized = serializeItemStacks(requiredItems);
        this.rewardsSerialized = serializeItemStacks(rewards);
        this.requiredItems = new ArrayList<>(requiredItems);
        this.rewards = new ArrayList<>(rewards);
        this.startTrader = startTrader;
        this.endTrader = endTrader;
    }

    public static TradeMissionSpec createRandomMission(Trader startTrader, Trader endTrader) {
        List<ItemStack> requiredItems = generateTradeItems(100.0f, 500.0f, 2, 4, TradeList.REQUIRED_ITEMS);
        List<ItemStack> rewards = generateTradeItems(100.0f, 1000.0f, 2, 4, TradeList.REWARDS);
        return new TradeMissionSpec(requiredItems, startTrader, endTrader, rewards);
    }

    public static TradeMissionSpec createFromStock(
            List<ItemStack> stockRequiredItems,
            List<ItemStack> stockRewards,
            Trader startTrader,
            Trader endTrader) {
        return new TradeMissionSpec(stockRequiredItems, startTrader, endTrader, stockRewards);
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
            if (generatedItems.size() >= uniqueItemsCount && totalValue < minValue) {
                // Find and remove the least valuable item stack
                ItemStack leastValuable = generatedItems.stream()
                        .min(Comparator.comparing(itemStackValues::get))
                        .get();

                totalValue -= itemStackValues.get(leastValuable);
                generatedItems.remove(leastValuable);
                itemStackValues.remove(leastValuable);
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
    
    private String serializeItemStacks(List<ItemStack> items) {
        Gson gson = new Gson();
        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (ItemStack item : items) {
            serializedItems.add(item.serialize());
        }
        return gson.toJson(serializedItems);
    }

    private List<ItemStack> deserializeItemStacks(String serialized) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> serializedItems = gson.fromJson(serialized, listType);
        List<ItemStack> items = new ArrayList<>();
        for (Map<String, Object> serializedItem : serializedItems) {
            items.add(ItemStack.deserialize(serializedItem));
        }
        return items;
    }

    // Update getters and setters to use the serialized fields
    public List<ItemStack> getRequiredItems() {
        if (requiredItems == null) {
            requiredItems = deserializeItemStacks(requiredItemsSerialized);
        }
        return requiredItems;
    }

    public void setRequiredItems(List<ItemStack> requiredItems) {
        this.requiredItemsSerialized = serializeItemStacks(requiredItems);
        this.requiredItems = new ArrayList<>(requiredItems);
    }

    public List<ItemStack> getRewards() {
        if (rewards == null) {
            rewards = deserializeItemStacks(rewardsSerialized);
        }
        return rewards;
    }

    public void setRewards(List<ItemStack> rewards) {
        this.rewardsSerialized = serializeItemStacks(rewards);
        this.rewards = new ArrayList<>(rewards);
    }

    public Trader getStartTrader() {
        return startTrader;
    }

    public Trader getEndTrader() {
        return endTrader;
    }

    public double getRouteDistance() {
        return startTrader.getLocation().distance(endTrader.getLocation());
    }
}
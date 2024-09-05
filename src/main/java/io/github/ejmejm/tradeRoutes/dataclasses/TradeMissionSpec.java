package io.github.ejmejm.tradeRoutes.dataclasses;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.github.ejmejm.tradeRoutes.ItemUtils;
import io.github.ejmejm.tradeRoutes.TradeConfig;
import org.bukkit.inventory.ItemStack;

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

    @DatabaseField
    private boolean taken;

    // Transient fields for quick access
    private transient List<ItemStack> requiredItems;
    private transient List<ItemStack> rewards;

    public TradeMissionSpec() {}

    public TradeMissionSpec(List<ItemStack> requiredItems, Trader startTrader, Trader endTrader, List<ItemStack> rewards) {
        this.requiredItemsSerialized = ItemUtils.serializeItemStacks(requiredItems);
        this.rewardsSerialized = ItemUtils.serializeItemStacks(rewards);
        this.requiredItems = new ArrayList<>(requiredItems);
        this.rewards = new ArrayList<>(rewards);
        this.startTrader = startTrader;
        this.endTrader = endTrader;
        this.taken = false;
    }

    public static TradeMissionSpec createRandomMission(Trader startTrader, Trader endTrader) {
        int traderLevel = startTrader.getLevel();

        // Base values
        float baseReqValue = TradeConfig.getFloat("base_requirements_value", traderLevel);
        float baseRewardValue = TradeConfig.getFloat("base_reward_value", traderLevel);
        float reqFluctuation = TradeConfig.getFloat("requirements_fluctuation_fraction", traderLevel);
        float rewardFluctuation = TradeConfig.getFloat("reward_fluctuation_fraction", traderLevel);
        int minReqItems = TradeConfig.getInt("min_requirements_unique_items", traderLevel);
        int maxReqItems = TradeConfig.getInt("max_requirements_unique_items", traderLevel);
        int minRewardItems = TradeConfig.getInt("min_reward_unique_items", traderLevel);
        int maxRewardItems = TradeConfig.getInt("max_reward_unique_items", traderLevel);
        
        // Distance scaling
        float minReqScalingDist = TradeConfig.getFloat("min_requirements_scaling_distance", traderLevel);
        float baseReqScalingDist = TradeConfig.getFloat("base_requirements_scaling_distance", traderLevel);
        float maxReqScalingDist = TradeConfig.getFloat("max_requirements_scaling_distance", traderLevel);
        float minReqScalingMult = TradeConfig.getFloat("min_requirements_scaling_multiplier", traderLevel);
        float maxReqScalingMult = TradeConfig.getFloat("max_requirements_scaling_multiplier", traderLevel);

        float minRewardScalingDist = TradeConfig.getFloat("min_reward_scaling_distance", traderLevel);
        float baseRewardScalingDist = TradeConfig.getFloat("base_reward_scaling_distance", traderLevel);
        float maxRewardScalingDist = TradeConfig.getFloat("max_reward_scaling_distance", traderLevel);
        float minRewardScalingMult = TradeConfig.getFloat("min_reward_scaling_multiplier", traderLevel);
        float maxRewardScalingMult = TradeConfig.getFloat("max_reward_scaling_multiplier", traderLevel);

        double distance = startTrader.getLocation().distance(endTrader.getLocation());

        float reqScalingMult = calculateScalingMultiplier(
            distance, minReqScalingDist, baseReqScalingDist, maxReqScalingDist, minReqScalingMult, maxReqScalingMult);
        float rewardScalingMult = calculateScalingMultiplier(
            distance, minRewardScalingDist, baseRewardScalingDist, maxRewardScalingDist, minRewardScalingMult, maxRewardScalingMult);

        baseReqValue *= reqScalingMult;
        baseRewardValue *= rewardScalingMult;

        float minReqValue = baseReqValue * (1 - reqFluctuation);
        float maxReqValue = baseReqValue * (1 + reqFluctuation);
        float minRewardValue = baseRewardValue * (1 - rewardFluctuation);
        float maxRewardValue = baseRewardValue * (1 + rewardFluctuation);

        List<ItemStack> requiredItems = generateTradeItems(minReqValue, maxReqValue, minReqItems, maxReqItems, TradeList.REQUIRED_ITEMS);
        List<ItemStack> rewards = generateTradeItems(minRewardValue, maxRewardValue, minRewardItems, maxRewardItems, TradeList.REWARDS);
        
        return new TradeMissionSpec(requiredItems, startTrader, endTrader, rewards);
    }
    
    /**
     * Calculates a scaling multiplier based on the given distance and scaling parameters.
     * 
     * This method computes a scaling multiplier that varies with distance. It uses a piecewise
     * linear function, with different calculations for distances below and above the base scaling distance.
     * The multiplier is clamped between minScalingMult and maxScalingMult.
     *
     * @param distance The distance to calculate the multiplier for.
     * @param minScalingDist The minimum distance for scaling.
     * @param baseScalingDist The base distance where scaling changes behavior.
     * @param maxScalingDist The maximum distance for scaling.
     * @param minScalingMult The minimum scaling multiplier.
     * @param maxScalingMult The maximum scaling multiplier.
     * @return The calculated scaling multiplier.
     */
    private static float calculateScalingMultiplier(
            double distance, 
            float minScalingDist, 
            float baseScalingDist, 
            float maxScalingDist, 
            float minScalingMult, 
            float maxScalingMult) {

        distance = Math.max(minScalingDist, Math.min(maxScalingDist, distance));

        float scaledMult;

        if (distance <= baseScalingDist) {
            float range = baseScalingDist - minScalingDist;
            float distAboveMin = (float) distance - minScalingDist;
            float ratio = distAboveMin / range;
            float multRange = 1.0f - minScalingMult;
            scaledMult = minScalingMult + (multRange * ratio);
        } else {
            float range = maxScalingDist - baseScalingDist;
            float distAboveBase = (float) distance - baseScalingDist;
            float ratio = distAboveBase / range;
            float multRange = maxScalingMult - 1.0f;
            scaledMult = 1.0f + (multRange * ratio);
        }
        return scaledMult;
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

    public int getId() {
        return id;
    }

    // Update getters and setters to use the serialized fields
    public List<ItemStack> getRequiredItems() {
        if (requiredItems == null) {
            requiredItems = ItemUtils.deserializeItemStacks(requiredItemsSerialized);
        }
        return requiredItems;
    }

    public void setRequiredItems(List<ItemStack> requiredItems) {
        this.requiredItemsSerialized = ItemUtils.serializeItemStacks(requiredItems);
        this.requiredItems = new ArrayList<>(requiredItems);
    }

    public List<ItemStack> getRewards() {
        if (rewards == null) {
            rewards = ItemUtils.deserializeItemStacks(rewardsSerialized);
        }
        return rewards;
    }

    public void setRewards(List<ItemStack> rewards) {
        this.rewardsSerialized = ItemUtils.serializeItemStacks(rewards);
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

    public boolean getTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }
}
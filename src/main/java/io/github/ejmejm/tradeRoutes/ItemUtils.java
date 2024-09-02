package io.github.ejmejm.tradeRoutes;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemUtils {

    public static String serializeItemStacks(List<ItemStack> items) {
        Gson gson = new Gson();
        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (ItemStack item : items) {
            serializedItems.add(item.serialize());
        }
        return gson.toJson(serializedItems);
    }

    public static List<ItemStack> deserializeItemStacks(String serialized) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> serializedItems = gson.fromJson(serialized, listType);
        List<ItemStack> items = new ArrayList<>();
        for (Map<String, Object> serializedItem : serializedItems) {
            items.add(ItemStack.deserialize(serializedItem));
        }
        return items;
    }

}

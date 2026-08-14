package me.abboycn.data;

import com.google.gson.*;
import me.abboycn.bot.StorageBotInventoryItem;
import me.abboycn.bot.StorageBotInventoryItemList;

import java.lang.reflect.Type;

public class StorageBotInventoryItemListTypeAdapter implements JsonDeserializer<StorageBotInventoryItemList> {
    @Override
    public StorageBotInventoryItemList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        StorageBotInventoryItemList list = new StorageBotInventoryItemList();

        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            for (JsonElement element : array) {
                StorageBotInventoryItem item = context.deserialize(element, StorageBotInventoryItem.class);
                if (item != null && item.getItem() != null) {
                    list.add(item);
                }
            }
        }

        return list;
    }
}
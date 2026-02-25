package me.abboycn.data;

import com.google.gson.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class InventorySerializer implements JsonSerializer<PlayerInventory>, JsonDeserializer<PlayerInventory> {
    @Override
    public JsonElement serialize(PlayerInventory inventory, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        JsonArray slots = new JsonArray();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                JsonObject slotJson = new JsonObject();
                slotJson.addProperty("slot", i);
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                slotJson.addProperty("item", itemId.toString());
                slotJson.addProperty("count", stack.getCount());
                slots.add(slotJson);
            }
        }
        json.add("slots", slots);
        return json;
    }

    @Override
    public PlayerInventory deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObj = json.getAsJsonObject();
        JsonArray slots = jsonObj.getAsJsonArray("slots");

        PlayerInventory inventory = new PlayerInventory(null);

        for (JsonElement slotElem : slots) {
            JsonObject slotJson = slotElem.getAsJsonObject();
            int slot = slotJson.get("slot").getAsInt();
            String itemId = slotJson.get("item").getAsString();
            int count = slotJson.get("count").getAsInt();

            Item item = Registries.ITEM.get(Identifier.of(itemId));
            ItemStack stack = new ItemStack(item, count);
            inventory.setStack(slot, stack);
        }
        return inventory;
    }
}
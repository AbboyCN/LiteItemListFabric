package me.abboycn.data;
import com.google.gson.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class InventorySerializer implements JsonSerializer<PlayerInventory>, JsonDeserializer<PlayerInventory> {
    private JsonObject serializeSingleStack(ItemStack innerStack, int slotIndex) {
        JsonObject obj = new JsonObject();
        obj.addProperty("slot", slotIndex);
        Identifier id = Registries.ITEM.getId(innerStack.getItem());
        obj.addProperty("item", id.toString());
        obj.addProperty("count", innerStack.getCount());

        if (innerStack.getComponents().contains(DataComponentTypes.CONTAINER)) {
            JsonArray childContainerSlots = new JsonArray();
            ContainerComponent container = innerStack.get(DataComponentTypes.CONTAINER);
            if(container!=null){
                for (int childSlotIdx = 0; childSlotIdx < 27; childSlotIdx++) {
                    ItemStack s = container.stream().toList().get(childSlotIdx);
                    if (!s.isEmpty()) {
                        childContainerSlots.add(serializeSingleStack(s, childSlotIdx));
                    }
                }
            }
            if (!childContainerSlots.isEmpty()) {
                obj.add("containerSlots", childContainerSlots);
            }
        }
        return obj;
    }

    private ItemStack deserializeSingleStack(JsonObject stackObj) {
        String itemIdStr = stackObj.get("item").getAsString();
        int cnt = stackObj.get("count").getAsInt();
        Item item = Registries.ITEM.get(Identifier.of(itemIdStr));
        ItemStack stack = new ItemStack(item, cnt);

        if (stackObj.has("containerSlots")) {
            JsonArray childArr = stackObj.getAsJsonArray("containerSlots");
            List<ItemStack> fullSlots = new ArrayList<>(27);
            for (int i = 0; i < 27; i++) {
                fullSlots.add(ItemStack.EMPTY);
            }
            for (JsonElement e : childArr) {
                JsonObject childObj = e.getAsJsonObject();
                int childSlot = childObj.get("slot").getAsInt();
                ItemStack childStack = deserializeSingleStack(childObj);
                fullSlots.set(childSlot, childStack);
            }
            ContainerComponent newContainer = ContainerComponent.fromStacks(fullSlots);
            stack.set(DataComponentTypes.CONTAINER, newContainer);
        }
        return stack;
    }

    @Override
    public JsonElement serialize(PlayerInventory inventory, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        JsonArray slots = new JsonArray();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                JsonObject slotJson = serializeSingleStack(stack, i);
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
            ItemStack stack = deserializeSingleStack(slotJson);
            inventory.setStack(slot, stack);
        }
        return inventory;
    }
}
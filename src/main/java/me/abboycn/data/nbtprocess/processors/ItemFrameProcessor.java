package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.nbtprocess.AbstractEntityProcessor;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class ItemFrameProcessor extends AbstractEntityProcessor {
    private static final String ITEM_FRAME_ID = "minecraft:item_frame";

    @Override
    public boolean process(NbtCompound entityNbt, Map<Item, Integer> itemCountMap) {
        addItem(itemCountMap, Items.ITEM_FRAME, 1);

        if (entityNbt.contains("Item", NbtElement.COMPOUND_TYPE)) {
            NbtCompound itemNbt = entityNbt.getCompound("Item");
            String itemId = itemNbt.getString("id");
            int count = itemNbt.getInt("count");

            if (itemId != null && !itemId.isEmpty()) {
                Item item = Registries.ITEM.get(Identifier.of(itemId));
                if (item != Items.AIR) {
                    addItem(itemCountMap, item, count);
                }
            }
        }
        return true;
    }

    @Override
    public boolean supports(String entityId) {
        return ITEM_FRAME_ID.equals(entityId);
    }
}

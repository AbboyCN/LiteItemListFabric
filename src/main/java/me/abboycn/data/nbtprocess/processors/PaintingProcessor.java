package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.nbtprocess.AbstractEntityProcessor;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public final class PaintingProcessor extends AbstractEntityProcessor {
    private static final String PAINTING_ID = "minecraft:painting";

    @Override
    public boolean process(NbtCompound entityNbt, Map<Item, Integer> itemCountMap) {
        addItem(itemCountMap, Items.PAINTING, 1);
        return true;
    }

    @Override
    public boolean supports(String entityId) {
        return PAINTING_ID.equals(entityId);
    }
}

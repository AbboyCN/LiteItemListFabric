package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.nbtprocess.AbstractEntityProcessor;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public final class ArmorStandProcessor extends AbstractEntityProcessor {
    private static final String ARMOR_STAND_ID = "minecraft:armor_stand";

    @Override
    public boolean process(NbtCompound entityNbt, Map<Item, Integer> itemCountMap) {
        addItem(itemCountMap, Items.ARMOR_STAND, 1);
        return true;
    }

    @Override
    public boolean supports(String entityId) {
        return ARMOR_STAND_ID.equals(entityId);
    }
}

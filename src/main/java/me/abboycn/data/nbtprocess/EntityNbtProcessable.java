package me.abboycn.data.nbtprocess;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public interface EntityNbtProcessable {
    boolean process(NbtCompound entityNbt, Map<Item, Integer> itemCountMap);

    default boolean supports(String entityId) {
        return false;
    }

    default String getName() {
        return this.getClass().getSimpleName();
    }
}

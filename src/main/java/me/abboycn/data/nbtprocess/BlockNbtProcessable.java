package me.abboycn.data.nbtprocess;

import me.abboycn.data.LitematicaReader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public interface BlockNbtProcessable {
    boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap);

    default boolean supports(Block block) {
        return false;
    }

    default String getName() {
        return this.getClass().getSimpleName();
    }
}

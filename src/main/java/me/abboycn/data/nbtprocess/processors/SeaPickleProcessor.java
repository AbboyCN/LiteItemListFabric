package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.AbstractBlockProcessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public final class SeaPickleProcessor extends AbstractBlockProcessor {
    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        int pickles = 1;
        String count = blockInfo.properties().get("pickles");
        if (count != null) {
            try {
                pickles = Integer.parseInt(count);
            } catch (NumberFormatException ignored) {}
        }
        return addItem(itemCountMap, Items.SEA_PICKLE, pickles);
    }

    @Override
    public boolean supports(Block block) {
        return block == Blocks.SEA_PICKLE;
    }
}

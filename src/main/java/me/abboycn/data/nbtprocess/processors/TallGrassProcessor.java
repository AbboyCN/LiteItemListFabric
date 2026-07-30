package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.AbstractBlockProcessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public final class TallGrassProcessor extends AbstractBlockProcessor {
    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        if ("lower".equals(blockInfo.properties().get("half"))) {
            return addItem(itemCountMap, Items.TALL_GRASS, 1);
        }
        return true;
    }

    @Override
    public boolean supports(Block block) {
        return block == Blocks.TALL_GRASS;
    }
}

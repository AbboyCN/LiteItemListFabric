package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.AbstractBlockProcessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public final class PinkPetalsProcessor extends AbstractBlockProcessor {

    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        int flowerAmount = 1;
        String count = blockInfo.properties().get("flower_amount");
        if (count != null) {
            try {
                flowerAmount = Integer.parseInt(count);
            } catch (NumberFormatException ignored) {}
        }
        return addItem(itemCountMap, Items.PINK_PETALS, flowerAmount);
    }

    @Override
    public boolean supports(Block block) {
        return block == Blocks.PINK_PETALS;
    }
}

package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.AbstractBlockProcessor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public final class TurtleEggProcessor extends AbstractBlockProcessor {
    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        int eggs = 1;
        String count = blockInfo.properties().get("eggs");
        if (count != null) {
            try {
                eggs = Integer.parseInt(count);
            } catch (NumberFormatException ignored) {}
        }
        return addItem(itemCountMap, Items.TURTLE_EGG, eggs);
    }

    @Override
    public boolean supports(Block block) {
        return block == Blocks.TURTLE_EGG;
    }
}

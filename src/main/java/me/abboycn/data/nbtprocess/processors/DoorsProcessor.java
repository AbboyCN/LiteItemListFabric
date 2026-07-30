package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.AbstractBlockProcessor;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class DoorsProcessor extends AbstractBlockProcessor {
    private static final TagKey<Block> DOORS = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of("minecraft", "doors")
    );

    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        if ("lower".equals(blockInfo.properties().get("half"))) {
            return addItem(itemCountMap, blockInfo.block().asItem(), 1);
        }
        return true;
    }

    @Override
    public boolean supports(Block block) {
        return block.getDefaultState().isIn(DOORS);
    }
}

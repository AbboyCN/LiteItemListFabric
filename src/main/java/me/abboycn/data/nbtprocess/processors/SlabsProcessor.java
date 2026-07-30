package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.AbstractBlockProcessor;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class SlabsProcessor extends AbstractBlockProcessor {
    private static final TagKey<Block> SLABS = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of("minecraft", "slabs")
    );

    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        Item slabItem = blockInfo.block().asItem();
        if (slabItem == null || slabItem == Items.AIR) return false;

        String type = blockInfo.properties().get("type");
        int amount = "double".equals(type) ? 2 : 1;
        return addItem(itemCountMap, slabItem, amount);
    }

    @Override
    public boolean supports(Block block) {
        return block.getDefaultState().isIn(SLABS);
    }
}

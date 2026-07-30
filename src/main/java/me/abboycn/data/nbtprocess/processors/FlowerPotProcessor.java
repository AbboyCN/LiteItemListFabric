package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.AbstractBlockProcessor;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class FlowerPotProcessor extends AbstractBlockProcessor {
    private static final String POTTED_PREFIX = "potted_";

    private static final TagKey<Block> FLOWER_POTS = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of("minecraft", "flower_pots")
    );

    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        addItem(itemCountMap, Items.FLOWER_POT, 1);
        Identifier id = Registries.BLOCK.getId(blockInfo.block());

        if (id.getPath().startsWith(POTTED_PREFIX)){
            Item plant = Registries.ITEM.get(Identifier.of(id.getNamespace(),id.getPath().substring(POTTED_PREFIX.length())));
            if(plant != Items.AIR){
                addItem(itemCountMap, plant, 1);
            }
        }
        return true;
    }

    @Override
    public boolean supports(Block block) {
        return block.getDefaultState().isIn(FLOWER_POTS);
    }
}

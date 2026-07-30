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

public final class CandlesProcessor extends AbstractBlockProcessor {
    private static final TagKey<Block> CANDLES = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of("minecraft", "candles")
    );

    @Override
    public boolean process(LitematicaReader.BlockStateInfo blockInfo, NbtCompound tileEntity, Map<Item, Integer> itemCountMap) {
        int candles = 1;
        String count = blockInfo.properties().get("candles");
        if (count != null) {
            try {
                candles = Integer.parseInt(count);
            } catch (NumberFormatException ignored) {}
        }
        return addItem(itemCountMap, Items.CANDLE, candles);
    }

    @Override
    public boolean supports(Block block) {
        return block.getDefaultState().isIn(CANDLES);
    }
}

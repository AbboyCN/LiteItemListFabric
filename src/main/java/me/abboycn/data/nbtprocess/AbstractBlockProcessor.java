package me.abboycn.data.nbtprocess;

import me.abboycn.data.LitematicaReader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;

import java.util.Map;

public abstract class AbstractBlockProcessor implements BlockNbtProcessable{
    protected boolean addItem(Map<Item, Integer> map, Item item, int amount) {
        return LitematicaReader.addItem(map, item, amount);
    }

    public boolean supportsTag(TagKey<Block> tag) {
        return false;
    }

    public TagKey<Block> getTag() {
        return null;
    }

    @Override
    public String getName() {
        return BlockNbtProcessable.super.getName();
    }
}

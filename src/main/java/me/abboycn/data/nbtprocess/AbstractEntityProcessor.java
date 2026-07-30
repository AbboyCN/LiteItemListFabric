package me.abboycn.data.nbtprocess;

import me.abboycn.data.LitematicaReader;
import net.minecraft.item.Item;

import java.util.Map;

public abstract class AbstractEntityProcessor implements EntityNbtProcessable{
    protected boolean addItem(Map<Item, Integer> map, Item item, int amount) {
        return LitematicaReader.addItem(map, item, amount);
    }

    @Override
    public String getName() {
        return EntityNbtProcessable.super.getName();
    }
}

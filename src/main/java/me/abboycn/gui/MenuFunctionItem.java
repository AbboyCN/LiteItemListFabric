package me.abboycn.gui;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public final class MenuFunctionItem {
    private final ItemStack itemStack;
    public MenuFunctionItem(Item item, Text name, List<Text> lore) {
        itemStack = new ItemStack(item);
        itemStack.set(DataComponentTypes.CUSTOM_NAME, name);
        itemStack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}

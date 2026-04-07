package me.abboycn.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class MenuSlot extends Slot {
    public MenuSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    // 禁止往这个槽放物品
    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    // 禁止从这个槽拿物品
    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        return false;
    }
}

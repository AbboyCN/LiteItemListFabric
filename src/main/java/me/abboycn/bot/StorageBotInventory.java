package me.abboycn.bot;

import com.google.gson.annotations.SerializedName;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class StorageBotInventory extends StorageBotInventoryItemList{
    @SerializedName("usedSlots")
    private int usedSlots = 0;
    public void syncFromInventory(Inventory inventory){
        usedSlots=0;
        this.clear();
        for(int i = 0; i < inventory.size(); i++){
            ItemStack itemStack = inventory.getStack(i);
            if(itemStack==null||itemStack.isEmpty()){
                continue;
            }
            if(itemStack.contains(DataComponentTypes.CONTAINER)){
                add(new StorageBotInventoryItem(itemStack.getItem(), itemStack.getCount(), parseContainerItemStack(itemStack)));
            }
            else{
                accumulateOrNew(itemStack.getItem(), itemStack.getCount());
            }
            usedSlots++;
        }
    }
    public int getUsedSlots(){
        return usedSlots;
    }
    public boolean isFull(){
        return usedSlots>=41;
    }
}

package me.abboycn.bot;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StorageBotInventoryItemList extends ArrayList<StorageBotInventoryItem> {
    public StorageBotInventoryItemList() {
        super();
    }
    public List<StorageBotInventoryItem> getInventoryItems() {return super.stream().toList();}
    public StorageBotInventoryItemList getAbsoluteInventoryItemList() {
        StorageBotInventoryItemList ret = new StorageBotInventoryItemList();
        this.forEach(inventoryItem -> ret.extend(inventoryItem.getUnpackedInventoryItemList()));
        return ret;
    }

    public StorageBotInventoryItemList parseContainerItemStack(ItemStack containerItemStack) {
        ContainerComponent container = containerItemStack.get(DataComponentTypes.CONTAINER);
        if(container==null){return null;}
        StorageBotInventoryItemList ret = new StorageBotInventoryItemList();
        for(ItemStack insideItemStack : container.stream().toList()){
            if(insideItemStack == null||insideItemStack.isEmpty()) continue;
            if(insideItemStack.contains(DataComponentTypes.CONTAINER)){
                ret.add(new StorageBotInventoryItem(insideItemStack.getItem(), insideItemStack.getCount(), parseContainerItemStack(insideItemStack)));
                continue;
            }
            accumulateOrNew(ret, insideItemStack.getItem(), insideItemStack.getCount());
        }
        return ret;
    }

    public static void accumulateOrNew(List<StorageBotInventoryItem> target, Item item, int count){
        target.stream().filter(inventoryItem -> inventoryItem.getItem().equals(item))
                .findFirst().ifPresentOrElse(inventoryItem -> inventoryItem.accumulate(count), ()-> target.add(new StorageBotInventoryItem(item,count)));
    }

    public StorageBotInventoryItemList accumulateOrNew(Item item, int count){
        accumulateOrNew(this, item, count);
        return this;
    }

    public static void extend(StorageBotInventoryItemList parent, StorageBotInventoryItemList other){
        other.forEach(inventoryItem -> {
            if(inventoryItem.getContainerItems()!=null){
                parent.add(new StorageBotInventoryItem(inventoryItem.getItem(), inventoryItem.getCount(), inventoryItem.getContainerItems()));
            }
            else {
                parent.accumulateOrNew(inventoryItem.getItem(), inventoryItem.getCount());
            }
        });
    }

    public StorageBotInventoryItemList extend(StorageBotInventoryItemList other){
        extend(this, other);
        return this;
    }

    public StorageBotInventoryItemList multiply(int i){
        if(i>0){this.forEach(inventoryItem -> inventoryItem.multiply(i));}
        return this;
    }

    public int count(Item item){
        return getAbsoluteInventoryItemList().stream().filter(inventoryItem -> inventoryItem.getItem().equals(item))
                .mapToInt(StorageBotInventoryItem::getCount).sum();
    }

    public StorageBotInventoryItem getInventoryItem(Item item){
        return this.stream().filter(inventoryItem -> inventoryItem.getItem().equals(item)).findFirst().orElse(null);
    }
}

package me.abboycn.bot;

import com.google.gson.annotations.SerializedName;
import net.minecraft.item.Item;

public class StorageBotInventoryItem {
    @SerializedName("item")
    private Item item;

    @SerializedName("count")
    private int count = 0;

    @SerializedName("containerItems")
    private StorageBotInventoryItemList containerItems = null;

    public StorageBotInventoryItem() {}

    public StorageBotInventoryItem(Item item, int count, StorageBotInventoryItemList containerItems) {
        this.item = item;
        this.count = count;
        this.containerItems = containerItems;
    }

    public StorageBotInventoryItem(Item item, int count) {
        this(item, count, null);
    }

    public Item getItem() {return item;}
    public int getCount() {return count;}
    public StorageBotInventoryItemList getContainerItems() {return containerItems;}

    public void accumulate(int count) {
        this.count += count;
    }

    public void multiply(int i) {
        if(i > 0) {
            this.count *= i;
        }
    }

    public StorageBotInventoryItemList getUnpackedInventoryItemList() {
        StorageBotInventoryItemList ret = new StorageBotInventoryItemList();
        ret.add(new StorageBotInventoryItem(item, count));

        if (containerItems != null && !containerItems.isEmpty()) {
            // 确保 containerItems 是 StorageBotInventoryItemList 类型
            StorageBotInventoryItemList containerList;
            if (containerItems instanceof StorageBotInventoryItemList) {
                containerList = containerItems;
            } else {
                containerList = new StorageBotInventoryItemList();
                containerList.addAll(containerItems);
            }

            for (StorageBotInventoryItem inventoryItem : containerList) {
                if (inventoryItem.getContainerItems() != null && !inventoryItem.getContainerItems().isEmpty()) {
                    ret.extend(inventoryItem.getUnpackedInventoryItemList().multiply(inventoryItem.getCount()));
                } else {
                    ret.accumulateOrNew(inventoryItem.getItem(), inventoryItem.getCount());
                }
            }
        }

        return ret;
    }
}
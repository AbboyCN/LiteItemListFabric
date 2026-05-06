package me.abboycn.task;

import com.google.gson.annotations.SerializedName;
import me.abboycn.bot.StorageBot;
import me.abboycn.bot.TaskStorageBotManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Objects;

public class TaskItemList {
    @SerializedName("project")
    private String project;
    @SerializedName("name")
    private String name;
    @SerializedName("filePath")
    private String filePath;
    @SerializedName("taskItems")
    private Collection<TaskItem> taskItems;

    // 空构造器（GSON反序列化需要）
    public TaskItemList() {
        this.taskItems = new ArrayList<>();
    }

    public TaskItemList(String filePath) {
        this.filePath = filePath;
        this.taskItems = new ArrayList<>();
    }

    public String getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Collection<TaskItem> getTaskItems() {
        return taskItems;
    }

    public TaskItem getTaskItem(Item item) {
        return taskItems.stream().filter(taskItem -> taskItem.getItem().equals(item)).findFirst().orElse(null);
    }

    public TaskItem getTaskItem(String itemId) {
        return taskItems.stream().filter(taskItem -> taskItem.getItem().equals(Registries.ITEM.get(Identifier.of(itemId)))).findFirst().orElse(null);
    }

    public boolean contains(Item item) {
        return taskItems.stream().anyMatch(taskItem -> taskItem.getItem().equals(item));
    }

    public int getTaskItemCount() {
        return taskItems.size();
    }

    public int getFinishedCount() {
        int ret = 0;
        for(TaskItem taskItem : taskItems) {
            if(taskItem.isFinished()) ret++;
        }
        return ret;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTaskItems(Collection<TaskItem> taskItems) {
        this.taskItems = taskItems;
    }

    // 添加物品到列表
    public void addTaskItem(TaskItem taskItem) {
        this.taskItems.add(taskItem);
    }

    public void calcAvailable(MinecraftServer server, TaskStorageBotManager botManager) {
        taskItems.forEach(taskItem -> taskItem.setAvailable(0));
        for(StorageBot bot : botManager.getBots()){
            bot.refreshInventory(server);
            Inventory inventory = bot.getInventory();
            for(TaskItem taskItem : taskItems) {
                for(int i=0; i<inventory.size(); i++){
                    if(inventory.getStack(i).getItem().equals(taskItem.getItem())){
                        taskItem.addAvailable(inventory.getStack(i).getCount());
                    }
                    else if(inventory.getStack(i).contains(DataComponentTypes.CONTAINER)){
                        taskItem.addAvailable(Objects.requireNonNull(inventory.getStack(i).get(DataComponentTypes.CONTAINER)).stream()
                                .filter(innerStack -> innerStack.isOf(taskItem.getItem()))
                                .mapToInt(ItemStack::getCount)
                                .sum());
                    }
                }
            }
        }
    }
}
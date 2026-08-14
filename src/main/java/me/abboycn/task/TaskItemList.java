package me.abboycn.task;

import com.google.gson.annotations.SerializedName;
import me.abboycn.bot.StorageBot;
import me.abboycn.bot.StorageBotInventory;
import me.abboycn.bot.TaskStorageBotManager;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.ArrayList;

public class TaskItemList {
    @SerializedName("project")
    private String project;
    @SerializedName("name")
    private String name;
    @SerializedName("filePath")
    private String filePath;
    @SerializedName("taskItems")
    private Collection<TaskItem> taskItems;

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

    public int getImptCount() {
        return (int)taskItems.stream().filter(TaskItem::isImpt).count();
    }

    public int getHardCount() {
        return (int)taskItems.stream().filter(TaskItem::isHard).count();
    }

    public int getFinishedCount() {
        return (int)taskItems.stream().filter(TaskItem::isFinished).count();
    }

    public int getUnclaimedCount() {
        return (int)taskItems.stream().filter(taskItem -> taskItem.getPrincipals().isEmpty()).count();
    }

    public int getOngoingCount() {
        return (int)taskItems.stream().filter(taskItem -> taskItem.getAvailable()>0&&!taskItem.isFinished()).count();
    }

    public int getNotStartCount() {
        return (int)taskItems.stream().filter(taskItem -> taskItem.getAvailable()==0).count();
    }

    public BigDecimal getProgressPercentage() {
        return ((taskItems.isEmpty())?BigDecimal.valueOf(0): BigDecimal.valueOf(getFinishedCount()/(double)taskItems.size()*100).setScale(2, RoundingMode.HALF_UP));
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
            StorageBotInventory inventory = bot.getInventory();
            for(TaskItem taskItem : taskItems) {
                taskItem.addAvailable(inventory.count(taskItem.getItem()));
            }
        }
    }
}
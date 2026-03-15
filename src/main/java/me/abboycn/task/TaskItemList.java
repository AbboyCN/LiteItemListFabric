package me.abboycn.task;

import com.google.gson.annotations.SerializedName;

import java.util.Collection;
import java.util.ArrayList;

public class TaskItemList {
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

    public String getName() {
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Collection<TaskItem> getTaskItems() {
        return taskItems;
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
}
package me.abboycn.task;

import com.google.gson.annotations.SerializedName;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;

public class TaskManager {
    @SerializedName("tasks")
    private Collection<ItemListTask> m_tasks = new ArrayList<>();
    @SerializedName("nextId")
    private int nextId = 0;

    public TaskManager() {
    }

    public TaskManager(Collection<ItemListTask> tasks) {
        this.m_tasks = tasks;
    }

    public void setNextId(int nextId) {
        this.nextId = nextId;
    }

    public boolean checkTaskExist(String name) {
        for (ItemListTask task : m_tasks) {
            if (task.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public ItemListTask newTask(String name, ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        if (checkTaskExist(name)) {
            player.sendMessage(Text.literal("§c任务\"" + name + "\"已存在,请终止当前任务或重命名新的任务!"));
            return null;
        }
        ItemListTask task = new ItemListTask(name, nextId++, player);
        task.getStorageBotManager().newBot().playerSummonFake(player);
        m_tasks.add(task);
        return task;
    }

    public Collection<ItemListTask> getTasks() {
        return m_tasks;
    }

    public ItemListTask getTask(String name) {
        return m_tasks.stream().filter(task -> task.getName().equals(name)).findFirst().orElse(null);
    }

    public ItemListTask getTask(int id) {
        return m_tasks.stream().filter(task -> task.getId() == id).findFirst().orElse(null);
    }

    public ItemListTask getTaskByPlayer(ServerPlayerEntity player) {
        return m_tasks.stream().filter(task -> player.getCommandTags().contains(task.getTaskCommandTag())).findFirst().orElse(null);
    }

    public String getTaskCommandTag(int id) {
        return "in_task_" + id;
    }

    public String getTaskCommandTag(String name) {
        return "in_task_" + getTask(name).getId();
    }

    public void deleteTask(ItemListTask task) {
        m_tasks.remove(task);
    }

    public void deleteTask(String name) {
        m_tasks.removeIf(task -> task.getName().equals(name));
    }

    public void deleteTask(int id) {
        m_tasks.remove(getTask(id));
    }
}

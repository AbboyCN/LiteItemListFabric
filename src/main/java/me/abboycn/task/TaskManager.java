package me.abboycn.task;

import com.google.gson.annotations.SerializedName;
import me.abboycn.data.DataVersion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
    @SerializedName("tasks")
    private List<ItemListTask> m_tasks;

    @SerializedName("nextId")
    private int nextId = 0;

    @SerializedName("version")
    private int fileVersion = DataVersion.CURRENT_VERSION;

    public TaskManager() {
        this.m_tasks = new ArrayList<>();
    }

    public int getNextId() {
        return nextId;
    }

    public void setNextId(int nextId) {
        this.nextId = nextId;
    }

    public int getVersion() {
        return fileVersion;
    }

    public boolean checkTaskExist(String name) {
        return m_tasks.stream().anyMatch(task -> task.getName().equals(name));
    }

    public ItemListTask newTask(String name, ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        ItemListTask task = new ItemListTask(name, nextId++, player);
        task.getStorageBotManager().newBot().playerSummonFake(player);
        m_tasks.add(task);
        return task;
    }

    public List<ItemListTask> getTasks() {
        return m_tasks;
    }

    public ItemListTask getTask(String name) {
        return m_tasks.stream()
                .filter(task -> task.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public ItemListTask getTask(int id) {
        return m_tasks.stream()
                .filter(task -> task.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public ItemListTask getTaskByPlayer(ServerPlayerEntity player) {
        if (player == null) return null;
        return m_tasks.stream()
                .filter(task -> player.getCommandTags().contains(task.getTaskCommandTag()))
                .findFirst()
                .orElse(null);
    }

    public String getTaskCommandTag(int id) {
        return "in_task_" + id;
    }

    public String getTaskCommandTag(String name) {
        ItemListTask task = getTask(name);
        return task != null ? "in_task_" + task.getId() : null;
    }

    public void deleteTask(ItemListTask task) {
        if (task != null) {
            m_tasks.remove(task);
        }
    }

    public void deleteTask(String name) {
        m_tasks.removeIf(task -> task.getName().equals(name));
    }

    public void deleteTask(int id) {
        ItemListTask task = getTask(id);
        if (task != null) {
            m_tasks.remove(task);
        }
    }

    public void startAutoRefreshAll(MinecraftServer server) {
        if (server == null) return;
        m_tasks.forEach(task -> task.startAutoRefreshStorage(server));
    }
}
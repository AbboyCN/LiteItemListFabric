package me.abboycn.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.TaskStorageBotManager;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskManager;
import net.minecraft.entity.player.PlayerInventory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DataPersistenceManager {
    private static final String STORAGE_PATH = "liteitemlist/";
    private static final String TASKS_FILE = STORAGE_PATH + "tasks.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().registerTypeAdapter(PlayerInventory.class, new InventorySerializer()).create();

    public static void initDirectory() {
        try {
            Files.createDirectories(Paths.get(STORAGE_PATH));
        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("failed to create data save folder:", e);
        }
    }

    public static void saveTasks() {
        try (FileWriter writer = new FileWriter(TASKS_FILE)) {
            TaskManager taskManager = LiteItemListFabric.taskManager;
            GSON.toJson(taskManager.getTasks(), writer);
            LiteItemListFabric.LOGGER.info("Successfully saved data of {} tasks.", taskManager.getTasks().size());
        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("failed to save task data:", e);
        }
    }

    public static void loadTasks() {
        File file = new File(TASKS_FILE);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            ItemListTask[] tasks = GSON.fromJson(reader, ItemListTask[].class);
            TaskManager taskManager = LiteItemListFabric.taskManager;

            taskManager.getTasks().clear();
            for (ItemListTask task : tasks) {
                taskManager.getTasks().add(task);
                if (task.getStorageBotManager() == null) {
                    task.setBotManager(new TaskStorageBotManager(task.getName()));
                }
            }
            LiteItemListFabric.LOGGER.info("loaded {} tasks from saved data.", tasks.length);
        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("failed to load task data:", e);
        }
    }
}
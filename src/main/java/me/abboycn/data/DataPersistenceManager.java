// me/abboycn/data/DataPersistenceManager.java
package me.abboycn.data;

import com.google.gson.*;
import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.StorageBotInventoryItemList;
import me.abboycn.task.TaskManager;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DataPersistenceManager {
    private static final String STORAGE_PATH = "liteitemlist/";
    private static final String TASKS_FILE = STORAGE_PATH + "tasks.json";
    private static final String BACKUP_SUFFIX = ".backup";

    public static final Gson GSON = createGson();

    private static Gson createGson() {
        return new GsonBuilder().setPrettyPrinting().serializeNulls()
                .registerTypeHierarchyAdapter(Item.class, new ItemTypeAdapter())
                .registerTypeAdapter(StorageBotInventoryItemList.class, new StorageBotInventoryItemListTypeAdapter())
                .create();
    }

    private static DataLoadState loadState = DataLoadState.NOT_LOADED;

    public enum DataLoadState {
        NOT_LOADED,
        SUCCESS,
        FAILED_NO_FILE,
        FAILED_VERSION_MISMATCH,
        FAILED_PARSE_ERROR
    }

    public static DataLoadState getLoadState() {
        return loadState;
    }

    public static boolean isDataLoaded() {
        return loadState == DataLoadState.SUCCESS;
    }

    public static void initDirectory() {
        try {
            Files.createDirectories(Paths.get(STORAGE_PATH));
        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("Failed to create data directory:", e);
        }
    }

    public static void saveTasks() {
        saveTasks(true);
    }

    public static void saveTasks(boolean printLog) {
        // 检查是否成功加载了数据
        if (loadState != DataLoadState.SUCCESS) {
            LiteItemListFabric.LOGGER.warn("Data not loaded successfully, skipping save.");
            return;
        }

        if (LiteItemListFabric.taskManager == null) {
            LiteItemListFabric.LOGGER.warn("TaskManager is null, skipping save.");
            return;
        }

        try (FileWriter writer = new FileWriter(TASKS_FILE)) {
            GSON.toJson(LiteItemListFabric.taskManager, writer);
            if (printLog) {
                LiteItemListFabric.LOGGER.info("Successfully saved {} task(s).",
                        LiteItemListFabric.taskManager.getTasks().size());
            }
        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("Failed to save task data:", e);
        }
    }

    public static void loadTasks(MinecraftServer server) {
        File file = new File(TASKS_FILE);

        if (!file.exists()) {
            LiteItemListFabric.LOGGER.info("No saved data found, creating new TaskManager.");
            LiteItemListFabric.taskManager = new TaskManager();
            loadState = DataLoadState.SUCCESS;
            return;
        }

        File backupFile = new File(TASKS_FILE + BACKUP_SUFFIX);
        if (backupFile.exists()) {
            LiteItemListFabric.LOGGER.info("Backup file found, attempting to restore...");
            if (loadFromFile(backupFile, server)) {
                LiteItemListFabric.LOGGER.info("Successfully restored from backup.");
                saveTasks(false);
                return;
            }
        }

        if (!loadFromFile(file, server)) {
            LiteItemListFabric.LOGGER.warn("Failed to load data, creating new TaskManager.");
            LiteItemListFabric.taskManager = new TaskManager();
            loadState = DataLoadState.SUCCESS;
        }
    }

    private static boolean loadFromFile(File file, MinecraftServer server) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            if (!jsonObject.has("version")) {
                LiteItemListFabric.LOGGER.error("No version field in data file.");
                loadState = DataLoadState.FAILED_VERSION_MISMATCH;
                return false;
            }

            int version = jsonObject.get("version").getAsInt();

            if (!DataVersion.isSupported(version)) {
                LiteItemListFabric.LOGGER.error("Unsupported version: {}, supported: {}-{}",
                        version, DataVersion.MIN_SUPPORTED_VERSION, DataVersion.MAX_SUPPORTED_VERSION);
                loadState = DataLoadState.FAILED_VERSION_MISMATCH;
                backupFile(file, "version_" + version);
                return false;
            }

            if (!DataVersion.isCurrent(version)) {
                LiteItemListFabric.LOGGER.info("Migrating data from version {} to {} (support in future version!)", version, DataVersion.CURRENT_VERSION);
            }

            TaskManager loaded = GSON.fromJson(jsonObject, TaskManager.class);

            if (loaded == null) {
                LiteItemListFabric.LOGGER.error("Deserialization returned null.");
                loadState = DataLoadState.FAILED_PARSE_ERROR;
                backupFile(file, "null_deserialization");
                return false;
            }

            if (LiteItemListFabric.taskManager != null) {
                LiteItemListFabric.taskManager.getTasks().clear();
            }
            LiteItemListFabric.taskManager = loaded;

            if (server != null) {
                LiteItemListFabric.taskManager.startAutoRefreshAll(server);
            }

            loadState = DataLoadState.SUCCESS;
            LiteItemListFabric.LOGGER.info("Loaded {} task(s) from saved data (version {}).",
                    LiteItemListFabric.taskManager.getTasks().size(), version);
            return true;

        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("Failed to read data file:", e);
            loadState = DataLoadState.FAILED_PARSE_ERROR;
            backupFile(file, "io_error");
            return false;
        } catch (JsonSyntaxException e) {
            LiteItemListFabric.LOGGER.error("Failed to parse JSON:", e);
            loadState = DataLoadState.FAILED_PARSE_ERROR;
            backupFile(file, "json_syntax_error");
            return false;
        } catch (Exception e) {
            LiteItemListFabric.LOGGER.error("Unexpected error loading data:", e);
            loadState = DataLoadState.FAILED_PARSE_ERROR;
            backupFile(file, "unexpected_error");
            return false;
        }
    }

    private static void backupFile(File file, String reason) {
        try {
            String backupName = String.format("tasks_backup_%s_%d.json", reason, System.currentTimeMillis());
            File backup = new File(file.getParent(), backupName);
            Files.copy(file.toPath(), backup.toPath());
            LiteItemListFabric.LOGGER.info("Backed up file to: {}", backup.getName());
        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("Failed to backup file:", e);
        }
    }
}
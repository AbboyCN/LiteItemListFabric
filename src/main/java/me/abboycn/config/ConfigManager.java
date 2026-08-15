package me.abboycn.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.abboycn.LiteItemListFabric;
import me.abboycn.resource.LangProvider;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    public static final ConfigManager INSTANCE = new ConfigManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "liteitemlist.cfg";

    private final File configFile;
    private LiteItemListConfig config;

    private ConfigManager() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        configFile = new File(configDir, CONFIG_FILE_NAME);
    }

    public LiteItemListConfig getConfig() {
        return config;
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            config = new LiteItemListConfig();
            saveConfig();
            LiteItemListFabric.LOGGER.info("Config file not found, Generated new config/liteitemlist.cfg");
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            config = GSON.fromJson(reader, LiteItemListConfig.class);
            LiteItemListFabric.LOGGER.info("Successfully loaded config/liteitemlist.cfg");
            LangProvider.setLang(config.getLanguage());
        } catch (Exception e) {
            LiteItemListFabric.LOGGER.error("Failed to read liteitemlist.cfg, liteitemlist will run with default config", e);
            config = new LiteItemListConfig();
        }
    }

    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            LiteItemListFabric.LOGGER.error("Failed to save liteitemlist.cfg", e);
        }
    }

    public void reloadConfig() {
        loadConfig();
    }
}
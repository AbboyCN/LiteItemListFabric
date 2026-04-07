package me.abboycn;

import me.abboycn.data.DataPersistenceManager;
import me.abboycn.event.*;
import me.abboycn.task.TaskManager;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static me.abboycn.command.CommandRegister.*;

public class LiteItemListFabric implements ModInitializer {
	public static final String MOD_ID = "liteitemlist-fabric";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static TaskManager taskManager = new TaskManager();

	@Override
	public void onInitialize() {
		DataPersistenceManager.initDirectory();

		registerCommands();

		ServerStopEventListener.register();
		ServerStartEventListener.register();
		ServerSaveEventListener.register();
		PlayerLogoutEventListener.register();
		PlayerAttackEventListener.register();

		LOGGER.info("Successfully initialized LiteItemListFabric");
	}
}
package me.abboycn.event;

import me.abboycn.data.DataPersistenceManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ServerSaveEventListener {
    public static void register() {
        ServerLifecycleEvents.BEFORE_SAVE.register(((server, flush, force) -> DataPersistenceManager.saveTasks()));
    }
}

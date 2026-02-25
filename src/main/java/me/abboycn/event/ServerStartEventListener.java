package me.abboycn.event;

import me.abboycn.data.DataPersistenceManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ServerStartEventListener {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> DataPersistenceManager.loadTasks());
    }
}

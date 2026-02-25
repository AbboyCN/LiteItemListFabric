package me.abboycn.event;

import me.abboycn.data.DataPersistenceManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ServerStopEventListener {
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> DataPersistenceManager.saveTasks());
    }
}

package me.abboycn.event;

import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.StorageBot;
import me.abboycn.task.ItemListTask;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerLogoutEventListener {
    public static void register(){
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if(player == null || !player.getCommandTags().contains("storage_bot")) return;
            for(ItemListTask itemListTask : LiteItemListFabric.taskManager.getTasks()){
                for(StorageBot bot : itemListTask.getStorageBotManager().getBots()){
                    if(bot == null) continue;
                    if(bot.getName().equals(player.getName().getString())){
                        bot.setInventory(player.getInventory());
                        break;
                    }
                }
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register(((entity, serverWorld) -> {
            if(!(entity instanceof ServerPlayerEntity player)) return;
            if(!player.getCommandTags().contains("storage_bot")) return;
            for(ItemListTask itemListTask : LiteItemListFabric.taskManager.getTasks()){
                for(StorageBot bot : itemListTask.getStorageBotManager().getBots()){
                    if(bot == null) continue;
                    if(bot.getName().equals(player.getName().getString())){
                        bot.setInventory(player.getInventory());
                        break;
                    }
                }
            }
        }));
    }
}

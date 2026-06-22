package me.abboycn.executor;

import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.data.DataPersistenceManager;
import me.abboycn.data.LitematicaReader;
import me.abboycn.resource.LangProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CMDReload {
    public static int CMDReloadExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null) return 0;
        return executeOperation(player,false);
    }

    public static int CMDForceReloadExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null) return 0;
        return executeOperation(player,true);
    }

    public static int executeOperation(ServerPlayerEntity player, boolean isForce){
        try{
            if(!isForce){
                DataPersistenceManager.saveTasks();
            }
            DataPersistenceManager.loadTasks();
            LitematicaReader.refreshFileList();
            player.sendMessage(LangProvider.get(isForce?"msg.liteitemlist.cmd.reload.force.success":"msg.liteitemlist.cmd.reload.success"), false);
            return 1;
        }
        catch(Exception e){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.reload.failed"));
            LiteItemListFabric.LOGGER.error("Error while loading tasks:", e);
            return 0;
        }
    }
}

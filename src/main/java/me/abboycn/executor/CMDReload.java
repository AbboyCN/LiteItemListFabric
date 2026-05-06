package me.abboycn.executor;

import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.data.DataPersistenceManager;
import me.abboycn.data.LitematicaReader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
            DataPersistenceManager.loadTasks(player.server);
            LitematicaReader.refreshFileList();
            player.sendMessage(Text.literal(Formatting.GREEN + "已" + (isForce?"强制":"") + "重载所有任务!"));
            return 1;
        }
        catch(Exception e){
            LiteItemListFabric.LOGGER.error("Error while loading tasks:", e);
            return 0;
        }
    }
}

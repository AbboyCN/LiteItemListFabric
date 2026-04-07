package me.abboycn.executor;

import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.gui.LiteItemListScreenHandler;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskItemList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CMDList {
    public static int CMDListExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        return showGUI(player);
    }

    public static int showGUI(ServerPlayerEntity player){
        return showGUI(player,LiteItemListFabric.taskManager.getTaskByPlayer(player));
    }

    public static int showGUI(ServerPlayerEntity player, ItemListTask itemListTask){
        if(player==null){
            return 0;
        }
        if(itemListTask==null){
            player.sendMessage(Text.literal("§c你没有参与任务."));
            return 0;
        }
        TaskItemList taskItemList = itemListTask.getItemList();
        if(taskItemList==null){
            return 0;
        }
        try{
            LiteItemListScreenHandler.openTaskItemMenu(player, itemListTask);
            return 1;
        }
        catch (Exception e){
            LiteItemListFabric.LOGGER.error("failed to open task item menu to {}",player.getName().getString(),e);
            return 0;
        }
    }
}

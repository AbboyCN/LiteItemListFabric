package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CMDTaskCancel {
    public static int CMDTaskCancelExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null){return 0;}
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if(task==null||task.getCreator()==null){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.unknown_task"));
            return 0;
        }
        if(player.getPermissionLevel()<2&&!task.getCreator().equals(player.getName().getString())){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.low_permission"));
            return 0;
        }
        for(ServerPlayerEntity onlinePlayer : context.getSource().getServer().getPlayerManager().getPlayerList()){
            if(onlinePlayer.getCommandTags().contains(task.getTaskCommandTag())){
                onlinePlayer.removeCommandTag(task.getTaskCommandTag());
                onlinePlayer.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.cancel.notice",task.getName(),player.getName().getString()));
            }
        }
        LiteItemListFabric.taskManager.deleteTask(task);
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.cancel.success"));
        return 1;
    }
}

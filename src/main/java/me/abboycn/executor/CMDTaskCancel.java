package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;


public class CMDTaskCancel {
    public static int CMDTaskCancelExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null){return 0;}
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if(task==null||task.getCreator()==null){
            player.sendMessage(Text.literal("§c未知的任务,请检查拼写或使用/liteitemlist task list查看任务列表!"));
            return 0;
        }
        if(player.getPermissionLevel()<2&&!task.getCreator().equals(player.getName().getString())){
            player.sendMessage(Text.literal("§c权限不足,你必须是任务创建者或管理员!"));
            return 0;
        }
        for(ServerPlayerEntity onlinePlayer : context.getSource().getServer().getPlayerManager().getPlayerList()){
            if(onlinePlayer.getCommandTags().contains(task.getTaskCommandTag())){
                onlinePlayer.removeCommandTag(task.getTaskCommandTag());
                onlinePlayer.sendMessage(Text.literal("§e你参与的任务\""+task.getName()+"\"已被玩家§b"+player.getName().getString()+"§e取消."));
            }
        }
        LiteItemListFabric.taskManager.deleteTask(task);
        player.sendMessage(Text.literal("§a任务已取消."));
        return 1;
    }
}

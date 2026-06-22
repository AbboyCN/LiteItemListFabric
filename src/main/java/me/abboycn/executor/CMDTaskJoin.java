package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CMDTaskJoin {
    public static int CMDTaskJoinExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null){return 0;}
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if(task==null||task.getCreator()==null){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.unknown_task"));
            return 0;
        }
        if(task.containsMember(player)){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.has_in_task"));
            return 1;
        }
        player.getCommandTags().removeIf(tag -> tag.contains("in_task_"));
        player.addCommandTag(task.getTaskCommandTag());
        task.addMember(player);
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.join.success",task.getName()));
        return 1;
    }
}

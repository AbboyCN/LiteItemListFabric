package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CMDTaskSwitch {
    public static int CMDTaskSwitchExecutor(CommandContext<ServerCommandSource> context){
        return executeOperation(context.getSource().getPlayer(), LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project")));
    }

    public static int executeOperation(ServerPlayerEntity player, ItemListTask task){
        if(player==null){return 0;}
        if(task==null||task.getCreator()==null){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.unknown_task"));
            return 0;
        }
        if(!task.containsMember(player)){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.not_in_task"));
            return 0;
        }
        player.getCommandTags().removeIf(tag -> tag.contains("in_task_"));
        player.getCommandTags().add(task.getTaskCommandTag());
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.switch.success",task.getName()));
        return 1;
    }
}

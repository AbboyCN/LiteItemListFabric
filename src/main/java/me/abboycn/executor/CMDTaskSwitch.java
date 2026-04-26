package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CMDTaskSwitch {
    public static int CMDTaskSwitchExecutor(CommandContext<ServerCommandSource> context){
        return executeOperation(context.getSource().getPlayer(), LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project")));
    }

    public static int executeOperation(ServerPlayerEntity player, ItemListTask task){
        if(player==null){return 0;}
        if(task==null||task.getCreator()==null){
            player.sendMessage(Text.literal("§c未知的任务,请检查拼写或使用/liteitemlist task list查看任务列表!"));
            return 0;
        }
        if(!task.containsMember(player)){
            player.sendMessage(Text.literal("§c你未参与此任务!"));
            return 0;
        }
        player.getCommandTags().removeIf(tag -> tag.contains("in_task_"));
        player.getCommandTags().add(task.getTaskCommandTag());
        player.sendMessage(Text.literal("§a已切换至任务:§e\""+task.getName()+"\""));
        return 1;
    }
}

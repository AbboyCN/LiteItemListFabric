package me.abboycn.executor;

import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CMDTaskList {
    public static int CMDTaskListExecutor(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        if (LiteItemListFabric.taskManager.getTasks().isEmpty()) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.list.no_tasks"));
            return 1;
        }
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.list.summary",LiteItemListFabric.taskManager.getTasks().size()));
        for (ItemListTask task : LiteItemListFabric.taskManager.getTasks()) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.list.name",task.getName()));
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.list.info",task.getCreator(),task.getMembers().size()));
        }
        return 1;
    }
}

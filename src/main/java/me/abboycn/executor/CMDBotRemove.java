package me.abboycn.executor;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.StorageBot;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CMDBotRemove {
    public static int CMDBotRemoveExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if (task == null || task.getCreator() == null) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.unknown_task"));
            return 0;
        }
        if (!task.containsMember(player)) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.not_in_task"));
            return 0;
        }
        int id = IntegerArgumentType.getInteger(context, "id");
        StorageBot bot = task.getStorageBotManager().getBot(id);
        if (bot == null) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.spawn.unknown_id"));
            return 0;
        }
        task.getStorageBotManager().removeBot(id);
        player.sendMessage(LangProvider.get("msg.liteitemlist.bot.remove.success",id));
        return 1;
    }
}

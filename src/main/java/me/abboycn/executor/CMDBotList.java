package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.StorageBot;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class CMDBotList {
    public static int CMDBotListExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null){return 0;}
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if(task==null||task.getCreator()==null){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.unknown_task"));
            return 0;
        }
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.list.summary", task.getStorageBotManager().getBots().size()));
        for(StorageBot bot : task.getStorageBotManager().getBots()){
            int usedStorage = bot.getUsedStorage(context.getSource().getServer());
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.list.bot_info", bot.getName(), String.valueOf(usedStorage==-1?"#NaN":usedStorage)));
        }
        return 1;
    }
}

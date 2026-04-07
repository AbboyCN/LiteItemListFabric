package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CMDChat {
    public static int CMDChatExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null) return 0;
        String msg = StringArgumentType.getString(context, "message");
        sendInTaskMessage(player, msg);
        return 1;
    }

    public static void sendInTaskMessage(ServerPlayerEntity player, String message){
        for(ItemListTask task : LiteItemListFabric.taskManager.getTasks()){
            if(player.getCommandTags().contains(task.getTaskCommandTag())){
                for(String member : task.getMembers()){
                    ServerPlayerEntity target = player.server.getPlayerManager().getPlayer(member);
                    if(target==null) continue;
                    target.sendMessage(Text.literal("§e<"+player.getName().getString()+"> §f"+message));
                }
            }
        }
    }
}

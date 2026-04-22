package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskMember;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
                for(TaskMember member : task.getMembers()){
                    ServerPlayerEntity target = player.server.getPlayerManager().getPlayer(member.getName());
                    if(target==null) continue;
                    target.sendMessage(Text.literal(Formatting.GOLD+"<"+player.getName().getString()+"> "+ Formatting.WHITE +message));
                }
            }
        }
    }
}

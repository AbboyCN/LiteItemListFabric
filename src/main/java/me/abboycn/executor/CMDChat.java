package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.command.CommandRegister;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskMember;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 管理 /liteitemlist chat 命令的执行。
 * <p>{@link CMDChat#CMDChatExecutor(CommandContext)} - 发送任务内信息
 * @see CommandRegister
 */
public final class CMDChat {
    /**
     * 发送任务内信息。
     * @param context CommandContext
     * @return 命令是否成功执行
     *
     * @implNote
     * <p>命令参数：
     * <p> - (GreedyString) msg - 信息
     * <p>执行者：仅玩家
     * <p>权限等级：member
     */
    public static int CMDChatExecutor(CommandContext<ServerCommandSource> context){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null) return 0;
        String msg = StringArgumentType.getString(context, "message");
        sendInTaskMessage(player, msg);
        return 1;
    }

    /**
     * 发送任务内信息。
     * <p>任务内信息的发送者游戏id将使用金色区别。
     * @param player 信息源玩家
     * @param message 信息内容
     */
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

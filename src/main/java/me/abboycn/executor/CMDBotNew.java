package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CMDBotNew {
    public static int CMDBotNewExecutor(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if (task == null || task.getCreator() == null) {
            player.sendMessage(Text.literal("§c未知的任务,请检查拼写或使用/liteitemlist task list查看任务列表!"));
            return 0;
        }
        if (!task.getMembers().contains(player.getName().getString())) {
            player.sendMessage(Text.literal("§c你未参与此任务!"));
            return 0;
        }
        task.getStorageBotManager().newBot().playerSummonFake(player);
        player.sendMessage(Text.literal("§a已创建并召唤新的存储假人."));
        return 1;
    }
}

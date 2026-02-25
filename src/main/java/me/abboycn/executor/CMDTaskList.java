package me.abboycn.executor;

import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CMDTaskList {
    public static int CMDTaskListExecutor(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        if (LiteItemListFabric.taskManager.getTasks().isEmpty()) {
            player.sendMessage(Text.literal("没有进行中的任务."));
            return 1;
        }
        player.sendMessage(Text.literal("共 " + LiteItemListFabric.taskManager.getTasks().size() + " 个进行中的任务:"));
        for (ItemListTask task : LiteItemListFabric.taskManager.getTasks()) {
            player.sendMessage(Text.literal(">§e" + task.getName()));
            player.sendMessage(Text.literal("  - 创建者:§b" + task.getCreator() + " §f参与者:§b" + task.getMembers().size() + "§f位玩家."));
        }
        return 1;
    }
}

package me.abboycn.executor;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.StorageBot;
import me.abboycn.bot.TaskStorageBotManager;
import me.abboycn.command.CommandRegister;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 管理 /liteitemlist bot 父命令下子命令的执行。
 * <p>{@link CMDBot#CMDBotListExecutor(CommandContext)} - 枚举假人
 * <p>{@link CMDBot#CMDBotNewExecutor(CommandContext)} - 新建假人
 * <p>{@link CMDBot#CMDBotRemoveExecutor(CommandContext)} - 移除假人
 * <p>{@link CMDBot#CMDBotSpawnExecutor(CommandContext, boolean)} - 召唤假人
 * @see CommandRegister
 * @see StorageBot
 * @see TaskStorageBotManager
 */

public final class CMDBot {
    /**
     * 枚举指定任务下的所有假人
     * @param context CommandContext
     * @return 命令是否成功执行
     *
     * @implNote
     * <p>命令参数：
     * <p> - (String) project - 任务名称
     * <p>执行者：仅玩家
     * <p>权限等级：default
     */
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
            int usedStorage = bot.getUsedStorage();
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.list.bot_info", bot.getName(), String.valueOf(usedStorage==-1?"#NaN":usedStorage)));
        }
        return 1;
    }

    /**
     * 创建一个新假人并召唤。
     * @param context CommandContext
     * @return 命令是否成功执行
     *
     * @implNote
     * <p>命令参数：
     * <p> - (String) project - 任务名称
     * <p>执行者：仅玩家
     * <p>权限等级：member, op
     */
    public static int CMDBotNewExecutor(CommandContext<ServerCommandSource> context) {
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
        task.getStorageBotManager().newBot().playerSummonFake(player);
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.new.success"));
        return 1;
    }

    /**
     * 从任务中移除假人。
     * <p>
     * 假人的移除只是相对于任务的，移除假人并不会导致假人下线或物品栏清空。
     * 假人的id具有唯一且不可复刻性，因此假人一旦移除，就不能再加入到任务中。
     *
     * @param context CommandContext
     * @return 命令是否成功执行
     *
     * @implNote
     * <p>命令参数：
     * <p> - (String) project - 任务名称
     * <p>执行者：仅玩家
     * <p>权限等级：member, op
     */
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

    /**
     * 召唤假人。
     * <p>
     * 如果 spawnAll 传入 true，则召唤任务中包含的每个假人。
     *
     * @param context CommandContext
     * @param spawnAll 是否全部召唤
     * @return 命令是否成功执行
     *
     * @implNote
     * <p>命令参数：
     * <p> - (String) project - 任务名称
     * <p> - [(int) id] - 假人id，留空将生成全部假人
     * <p>执行者：仅玩家
     * <p>权限等级：member, op
     */
    public static int CMDBotSpawnExecutor(CommandContext<ServerCommandSource> context, boolean spawnAll) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if (task == null || task.getCreator() == null) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.unknown_task"));
            return 0;
        }
        if (!(task.containsMember(player)||player.hasPermissionLevel(3))) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.not_in_task"));
            return 0;
        }
        if (spawnAll) {
            task.getStorageBotManager().summonAllBots(player);
            return 1;
        }
        int id = IntegerArgumentType.getInteger(context, "id");
        StorageBot bot = task.getStorageBotManager().getBot(id);
        if (bot == null) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.spawn.unknown_id"));
            return 0;
        }
        bot.playerSummonFake(player);
        return 1;
    }

    /**
     * 下线假人。
     * <p>
     * 如果 despawnAll 传入 true，则下线任务中包含的每个假人。
     *
     * @param context CommandContext
     * @param despawnAll 是否全部下线
     * @return 命令是否成功执行
     *
     * @implNote
     * <p>命令参数：
     * <p> - (String) project - 任务名称
     * <p> - [(int) id] - 假人id，留空将下线全部假人
     * <p>执行者：仅玩家
     * <p>权限等级：member, op
     */
    public static int CMDBotDespawnExecutor(CommandContext<ServerCommandSource> context, boolean despawnAll) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context, "project"));
        if (task == null || task.getCreator() == null) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.unknown_task"));
            return 0;
        }
        if (!(task.containsMember(player)||player.hasPermissionLevel(3))) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.not_in_task"));
            return 0;
        }
        if (despawnAll) {
            task.getStorageBotManager().despawnAllBots(player);
            return 1;
        }
        int id = IntegerArgumentType.getInteger(context, "id");
        StorageBot bot = task.getStorageBotManager().getBot(id);
        if (bot == null) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.spawn.unknown_id"));
            return 0;
        }
        if (bot.despawn(player.server)) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.despawn.success",bot.getName()));
            return 1;
        }
        else {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.despawn.offline"));
            return 0;
        }
    }
}

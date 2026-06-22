package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.data.LitematicaReader;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskItemList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.nio.file.Paths;

public class CMDTaskNew {
    public static int CMDTaskNewExecutor(CommandContext<ServerCommandSource> context, boolean loadFile){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null) return 0;
        String name = StringArgumentType.getString(context,"project");
        if(name.length()>13){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.new.failed.name_to_long"));
            return 0;
        }
        if(LiteItemListFabric.taskManager.checkTaskExist(name)){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.new.failed.exist",name));
            return 0;
        }
        ItemListTask task = LiteItemListFabric.taskManager.newTask(name, player);
        if(task==null){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.new.failed"));
            return 0;
        }
        if(loadFile){
            String argName = StringArgumentType.getString(context,"file");
            String strName = LitematicaReader.getFileName(argName);
            File file = Paths.get(strName).toFile();
            try {
                TaskItemList taskItemList = LitematicaReader.parseLitematicaFile(file);
                taskItemList.setProject(name);
                task.setItemList(taskItemList);
            } catch (Exception e) {
                LiteItemListFabric.LOGGER.error("Exception while reading litematic file: {}",file,e);
            }
        }
        player.getCommandTags().removeIf(tag -> tag.contains("in_task_"));
        player.addCommandTag(task.getTaskCommandTag());
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.task.new.success",name));
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.bot.new.success"));
        return 1;
    }
}

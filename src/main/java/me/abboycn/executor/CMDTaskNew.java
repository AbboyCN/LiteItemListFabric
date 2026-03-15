package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.data.LitematicaReader;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskItemList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.nio.file.Paths;

public class CMDTaskNew {
    public static int CMDTaskNewExecutor(CommandContext<ServerCommandSource> context, boolean loadFile){
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null){return 0;}
        String name = StringArgumentType.getString(context,"project");
        if(name.length()>13){
            player.sendMessage(Text.literal("§c任务名称过长,应少于13字符!"));
            return 0;
        }
        ItemListTask task = LiteItemListFabric.taskManager.newTask(name, player);
        if(task==null){
            player.sendMessage(Text.literal("§c任务创建失败!"));
            return 0;
        }
        if(loadFile){
            String argName = StringArgumentType.getString(context,"file");
            String strName = LitematicaReader.getFileName(argName);
            File file = Paths.get(strName).toFile();
            try {
                TaskItemList taskItemList = LitematicaReader.parseLitematicaFile(file);
                task.setItemList(taskItemList);
            } catch (Exception e) {
                LiteItemListFabric.LOGGER.error("Exception while reading litematic file: {}",file,e);
            }
        }
        player.getCommandTags().removeIf(tag -> tag.contains("in_task_"));
        player.addCommandTag(task.getTaskCommandTag());
        player.sendMessage(Text.literal("§a已创建任务:§e\""+name+"\""));
        player.sendMessage(Text.literal("§a已召唤新的存储假人."));
        return 1;
    }
}

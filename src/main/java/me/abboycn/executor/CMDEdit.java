package me.abboycn.executor;

import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CMDEdit {
    public static int CMDEditAddExecutor(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if(player==null){return 0;}
        ItemListTask task = LiteItemListFabric.taskManager.getTaskByPlayer(player);
        if(task==null){
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.not_in_task"));
            return 0;
        }
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        if(item==null||item== Items.AIR){return 0;}
        //TODO: ADD LOGIC.
        //FIXME:
        return 1;
    }
}

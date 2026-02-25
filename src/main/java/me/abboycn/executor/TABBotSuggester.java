package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.StorageBot;
import me.abboycn.task.ItemListTask;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class TABBotSuggester {
    public static CompletableFuture<Suggestions> botSuggester(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
        String input = builder.getRemainingLowerCase();
        ItemListTask task = LiteItemListFabric.taskManager.getTask(StringArgumentType.getString(context,"project"));
        if(task==null){return Suggestions.empty();}
        for(StorageBot bot : task.getStorageBotManager().getBots()){
            if(Integer.toString(bot.getId()).contains(input)){
                builder.suggest(bot.getId());
            }
        }
        return builder.buildFuture();
    }
}

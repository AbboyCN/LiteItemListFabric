package me.abboycn.executor;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.abboycn.LiteItemListFabric;
import me.abboycn.task.ItemListTask;

import java.util.concurrent.CompletableFuture;

public class TABTaskSuggester {
    public static CompletableFuture<Suggestions> projectSuggester(SuggestionsBuilder builder){
        String input = builder.getRemainingLowerCase();
        for(ItemListTask task : LiteItemListFabric.taskManager.getTasks()){
            if(task.getName().contains(input)){
                builder.suggest(task.getName());
            }
        }
        return builder.buildFuture();
    }
}

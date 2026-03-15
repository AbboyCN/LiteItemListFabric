package me.abboycn.executor;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.abboycn.data.LitematicaReader;

import java.util.concurrent.CompletableFuture;

public class TABLitematicaSuggester {
    public static CompletableFuture<Suggestions> litematicaFileSuggester(SuggestionsBuilder builder) {
        String input = builder.getRemainingLowerCase();
        if(input.isEmpty()) {
            LitematicaReader.refreshFileList();
        }
        for (String fileName : LitematicaReader.getLitematicaFileNames()) {
            String suggestionName = LitematicaReader.getSuggestionName(fileName);
            if (suggestionName.toLowerCase().contains(input)) {
                builder.suggest(String.format("\"%s\"", suggestionName));
            }
        }
        return builder.buildFuture();
    }
}
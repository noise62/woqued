package worst.woqued.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import worst.woqued.api.module.ModuleManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StrictlyFeatureNameArgument implements ArgumentType<String> {

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String input = StringArgumentType.string().parse(reader);
        return input.replace("_", " ");
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> featureNames = ModuleManager.getInstance().getModules().stream()
                .map(m -> m.getName().replace(" ", "_"))
                .toList();

        String remaining = builder.getRemaining().toLowerCase();
        featureNames.stream()
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("ClickGUI", "Sprint", "Speed");
    }
}
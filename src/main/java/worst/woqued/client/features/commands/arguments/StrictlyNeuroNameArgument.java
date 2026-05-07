package worst.woqued.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import worst.woqued.client.features.modules.combat.NeuroRotationManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StrictlyNeuroNameArgument implements ArgumentType<String> {
    public static StrictlyNeuroNameArgument create() {
        return new StrictlyNeuroNameArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        NeuroRotationManager manager = NeuroRotationManager.getInstance();
        return CommandSource.suggestMatching(manager.getAvailableModels(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        List<String> models = NeuroRotationManager.getInstance().getAvailableModels();
        return models.isEmpty() ? List.of("model1", "model2") : models.stream().limit(5).toList();
    }
}
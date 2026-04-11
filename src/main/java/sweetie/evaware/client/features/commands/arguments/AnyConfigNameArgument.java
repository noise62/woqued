package sweetie.evaware.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import sweetie.evaware.api.system.configs.ConfigManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AnyConfigNameArgument implements ArgumentType<String> {
    public static AnyConfigNameArgument create() {
        return new AnyConfigNameArgument();
    }

    @Override
    public String parse(StringReader reader) {
        try {
            return reader.readString();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(ConfigManager.getInstance().getConfigsNames(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        List<String> examples = ConfigManager.getInstance().getConfigsNames().stream().limit(5).collect(Collectors.toList());
        return examples.isEmpty() ? List.of("eva", "donyka", "sex", "swag", "paris") : examples;
    }
}

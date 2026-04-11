package sweetie.evaware.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import sweetie.evaware.api.system.configs.ConfigManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StrictlyConfigNameArgument implements ArgumentType<String> {
    public static StrictlyConfigNameArgument create() {
        return new StrictlyConfigNameArgument();
    }

    @Override
    public String parse(StringReader reader) {
        String configName = null;
        try {
            configName = reader.readString();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        if (!ConfigManager.getInstance().getConfigsNames().contains(configName)) {
            try {
                throw new DynamicCommandExceptionType(name -> Text.literal("Конфиг с именем " + name + " не найден")).create(configName);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return configName;
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
package sweetie.evaware.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import sweetie.evaware.api.system.configs.MacroManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StrictlyMacroNameArgument implements ArgumentType<String> {

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return StringArgumentType.string().parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> macroNames = MacroManager.getInstance().getMacros().stream().map(MacroManager.Macro::getName).toList();

        String remaining = builder.getRemaining().toLowerCase();
        macroNames.stream()
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("home", "login", "tp");
    }
}
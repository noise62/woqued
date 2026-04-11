package sweetie.evaware.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public class AnyStringArgument implements ArgumentType<String> {
    public static AnyStringArgument create() {
        return new AnyStringArgument();
    }

    @Override
    public String parse(StringReader r) {
        int start = r.getCursor();
        while (r.canRead() && !Character.isWhitespace(r.peek())) r.skip();
        return r.getString().substring(start, r.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return builder.buildFuture();
    }
}

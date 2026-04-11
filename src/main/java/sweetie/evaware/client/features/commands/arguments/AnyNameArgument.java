package sweetie.evaware.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnyNameArgument implements ArgumentType<String> {
    public static AnyNameArgument create() {
        return new AnyNameArgument();
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
        MinecraftClient client = MinecraftClient.getInstance();
        List<String> playerNames = new ArrayList<>();
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().getPlayerList().forEach(entry -> playerNames.add(entry.getProfile().getName()));
        }
        return CommandSource.suggestMatching(playerNames, builder);
    }

    @Override
    public Collection<String> getExamples() {
        MinecraftClient client = MinecraftClient.getInstance();
        List<String> examples = new ArrayList<>();

        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().getPlayerList().stream().limit(5).forEach(entry -> examples.add(entry.getProfile().getName()));
        }

        if (examples.isEmpty()) {
            examples.addAll(List.of("Evelina", "Donya"));
        }

        return examples;
    }
}

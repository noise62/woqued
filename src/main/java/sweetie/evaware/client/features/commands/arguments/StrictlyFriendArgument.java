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
import sweetie.evaware.api.system.configs.FriendManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StrictlyFriendArgument implements ArgumentType<String> {
    public static StrictlyFriendArgument create() {
        return new StrictlyFriendArgument();
    }

    @Override
    public String parse(StringReader reader) {
        String friend = null;
        try {
            friend = reader.readString();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        if (!FriendManager.getInstance().contains(friend)) {
            try {
                throw new DynamicCommandExceptionType(name -> Text.literal("Друга с именем " + name + " не найдено")).create(friend);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return friend;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(FriendManager.getInstance().getData(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        List<String> examples = FriendManager.getInstance().getData().stream().limit(5).collect(Collectors.toList());
        return examples.isEmpty() ? List.of("eva", "donyka", "sex", "swag", "paris") : examples;
    }
}

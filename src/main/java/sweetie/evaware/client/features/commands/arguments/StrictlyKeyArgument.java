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
import org.lwjgl.glfw.GLFW;
import sweetie.evaware.api.system.backend.KeyStorage;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StrictlyKeyArgument implements ArgumentType<String> {
    public static StrictlyKeyArgument create() {
        return new StrictlyKeyArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String key = reader.readString();
        if (KeyStorage.getBind(key) == -1) {
            throw new DynamicCommandExceptionType(
                    name -> Text.literal("Клавиша " + name.toString() + " не найдена")
            ).create(key);
        }
        return key;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(
                Stream.of(GLFW.class.getDeclaredFields())
                        .filter(f -> f.getName().startsWith("GLFW_KEY_"))
                        .map(f -> f.getName().substring("GLFW_KEY_".length()))
                        .collect(Collectors.toList()),
                builder
        );
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("A", "B", "C", "D", "E");
    }
}
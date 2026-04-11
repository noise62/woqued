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
import sweetie.evaware.api.system.configs.StaffManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StrictlyStaffArgument implements ArgumentType<String> {
    public static StrictlyStaffArgument create() {
        return new StrictlyStaffArgument();
    }

    @Override
    public String parse(StringReader reader) {
        String staff = null;
        try {
            staff = reader.readString();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        if (!StaffManager.getInstance().contains(staff)) {
            try {
                throw new DynamicCommandExceptionType(name -> Text.literal("Лоха с именем " + name + " не найдено")).create(staff);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return staff;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(StaffManager.getInstance().getData(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        List<String> examples = StaffManager.getInstance().getData().stream().limit(5).collect(Collectors.toList());
        return examples.isEmpty() ? List.of("eva", "donyka", "sex", "swag", "paris") : examples;
    }
}

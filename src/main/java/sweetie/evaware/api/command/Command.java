package sweetie.evaware.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.Getter;
import net.minecraft.command.CommandSource;
import sweetie.evaware.api.system.interfaces.QuickImports;

@Getter
public abstract class Command implements QuickImports {
    private final String name;
    public int SINGLE_SUCCESS = 1;

    public Command() {
        CommandRegister metadata = getClass().getAnnotation(CommandRegister.class);
        this.name = metadata.name();
    }

    public abstract void execute(LiteralArgumentBuilder<CommandSource> builder);

    public final void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(name);
        execute(builder);
        dispatcher.register(builder);
    }

    public static <T> RequiredArgumentBuilder<CommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }
}

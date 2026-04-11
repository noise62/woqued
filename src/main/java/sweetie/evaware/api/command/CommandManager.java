package sweetie.evaware.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.client.features.commands.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager {
    @Getter private static final CommandManager instance = new CommandManager();

    private final CommandDispatcher<CommandSource> dispatcher;
    private final ClientCommandSource source;

    public CommandManager() {
        this.dispatcher = new CommandDispatcher<>();
        this.source = new ClientCommandSource(null, MinecraftClient.getInstance());
    }

    private final List<Command> commands = new ArrayList<>();

    public void load() {
        register(
                new CommandConfig(), new CommandFriend(), new CommandStaffs(),
                new CommandMacro(), new CommandGps(),
                new CommandSkin()
        );
    }

    public void register(Command... commands) {
        for (Command command : commands) {
            command.register(dispatcher);
            this.commands.add(command);
        }
    }

    public String getPrefix() {
        return "$";
    }

    public void executeCommands(String message, CallbackInfo ci) {
        if (message.startsWith(getPrefix())) {
            try {
                getDispatcher().execute(message.substring(getPrefix().length()), getSource());
            } catch (CommandSyntaxException ignored) {

            }

            ci.cancel();
        }
    }
}
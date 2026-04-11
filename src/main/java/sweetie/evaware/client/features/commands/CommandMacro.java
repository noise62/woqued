package sweetie.evaware.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import sweetie.evaware.api.command.Command;
import sweetie.evaware.api.command.CommandRegister;
import sweetie.evaware.api.system.backend.KeyStorage;
import sweetie.evaware.api.system.configs.MacroManager;
import sweetie.evaware.client.features.commands.arguments.AnyStringArgument;
import sweetie.evaware.client.features.commands.arguments.StrictlyKeyArgument;
import sweetie.evaware.client.features.commands.arguments.StrictlyMacroNameArgument;

@CommandRegister(name = "macro")
public class CommandMacro extends Command {
    private final MacroManager macroManager = MacroManager.getInstance();

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(argument("name", new AnyStringArgument()).then(argument("key", new StrictlyKeyArgument()).then(argument("message", StringArgumentType.greedyString()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");
            String keyName = StringArgumentType.getString(context, "key");
            String message = StringArgumentType.getString(context, "message");

            int keyCode = KeyStorage.getBind(keyName);
            if (keyCode == -1) {
                print("Клавиша " + keyName + " не найдена!");
                return 0;
            }

            if (macroManager.has(name)) {
                print("Макрос с таким именем уже существует!");
                return 0;
            }

            macroManager.add(name, message, keyCode);
            print("Добавлен макрос с названием " + name + " с кнопкой " + keyName + " с командой " + message);
            return SINGLE_SUCCESS;
        })))));

        builder.then(literal("remove").then(argument("name", new StrictlyMacroNameArgument()).executes(context -> {
            String name = StringArgumentType.getString(context, "name");

            if (!macroManager.has(name)) {
                print("Макрос с таким именем не найден!");
                return 0;
            }

            macroManager.remove(name);
            print("Макрос " + name + " был успешно удален!");

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("clear").executes(context -> {
            macroManager.getMacros().clear();

            print("Все макросы были удалены.");

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(context -> {
            if (macroManager.getMacros().isEmpty()) {
                print("Список пустой");
                return 0;
            }

            macroManager.getMacros().forEach(macro -> {
                print("Название: " + Formatting.GRAY + macro.getName() +
                        Formatting.RESET + ", Команда: " + Formatting.GRAY + macro.getMessage() +
                        Formatting.RESET + ", Кнопка: " + Formatting.GRAY +
                        KeyStorage.getBind(macro.getKey()));
            });

            return SINGLE_SUCCESS;
        }));
    }
}


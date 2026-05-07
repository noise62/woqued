package worst.woqued.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import worst.woqued.api.command.Command;
import worst.woqued.api.command.CommandRegister;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleManager;
import worst.woqued.api.system.backend.KeyStorage;
import worst.woqued.api.system.configs.BindManager;
import worst.woqued.client.features.commands.arguments.StrictlyFeatureNameArgument;
import worst.woqued.client.features.commands.arguments.StrictlyKeyArgument;

import java.util.Optional;

@CommandRegister(name = "bind")
public class CommandBind extends Command {
    private final BindManager bindManager = BindManager.getInstance();

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(argument("feature", new StrictlyFeatureNameArgument()).then(argument("key", new StrictlyKeyArgument()).executes(context -> {
            String feature = context.getArgument("feature", String.class);

            Optional<Module> module = ModuleManager.getInstance().getModules().stream()
                    .filter(m -> m.getName().equalsIgnoreCase(feature))
                    .findFirst();

            if (module.isEmpty()) {
                print("Модуль " + feature + " не найден!");
                return 0;
            }

            String keyName = context.getArgument("key", String.class);
            int keyCode = KeyStorage.getBind(keyName);
            if (keyCode == -1) {
                print("Клавиша " + keyName + " не найдена!");
                return 0;
            }

            String actualName = module.get().getName();
            bindManager.add(actualName, keyCode);
            print("Бинд " + actualName + " -> " + keyName + " добавлен");

            return SINGLE_SUCCESS;
        }))));

        builder.then(literal("off").then(argument("feature", new StrictlyFeatureNameArgument()).executes(context -> {
            String feature = context.getArgument("feature", String.class);

            Optional<Module> module = ModuleManager.getInstance().getModules().stream()
                    .filter(m -> m.getName().equalsIgnoreCase(feature))
                    .findFirst();

            if (module.isEmpty()) {
                print("Модуль " + feature + " не найден!");
                return 0;
            }

            if (!bindManager.has(feature)) {
                print("Бинд для " + feature + " не найден!");
                return 0;
            }

            String actualName = module.get().getName();
            bindManager.remove(actualName);
            print("Бинд для " + actualName + " убран");

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(context -> {
            boolean hasAny = false;

            for (Module module : ModuleManager.getInstance().getModules()) {
                int bindKey = module.getBind();
                if (bindKey == -999 || bindKey == -1) continue;

                print(Formatting.GRAY + module.getName() + Formatting.RESET + " -> " + Formatting.GRAY + KeyStorage.getBind(bindKey));
                hasAny = true;
            }

            if (!hasAny) {
                print("Список биндов пустой");
            }

            return SINGLE_SUCCESS;
        }));
    }
}
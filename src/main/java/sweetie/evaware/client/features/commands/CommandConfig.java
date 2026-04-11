package sweetie.evaware.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import sweetie.evaware.api.command.Command;
import sweetie.evaware.api.command.CommandRegister;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.system.configs.ConfigManager;
import sweetie.evaware.client.features.commands.arguments.AnyConfigNameArgument;
import sweetie.evaware.client.features.commands.arguments.StrictlyConfigNameArgument;

import java.util.Collection;

@CommandRegister(name = "config")
public class CommandConfig extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("list").executes(context -> {
            Collection<String> configs = ConfigManager.getInstance().getConfigsNames();
            if (configs.isEmpty()) {
                print("Список конфигов пуст.");
            } else {
                print("Список конфигов: " + String.join(", ", configs));
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("remove")
                .then(argument("config", StrictlyConfigNameArgument.create())
                        .executes(context -> {
                            String config = StringArgumentType.getString(context, "config");
                            if (ConfigManager.getInstance().exists(config)) {
                                ConfigManager.getInstance().remove(config);
                                print("Конфиг " + config + " удалён.");
                            } else {
                                print("Я не нашла такого конфига.");
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("load")
                .then(argument("config", StrictlyConfigNameArgument.create())
                        .executes(context -> {
                            String config = StringArgumentType.getString(context, "config");
                            ConfigManager.getInstance().load(config);
                            if (ConfigManager.getInstance().exists(config)) {
                                print("Загружен конфиг: " + config);
                            } else {
                                print("Я не нашла такого конфига T.T");
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("save")
                .then(argument("config", AnyConfigNameArgument.create())
                        .executes(context -> {
                            String config = StringArgumentType.getString(context, "config");
                            ConfigManager.getInstance().save(config);
                            print("Сохранён конфиг: " + config);
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("folder").executes(context -> {
            if (SharedClass.openFolder(ClientInfo.CONFIG_PATH_MAIN)) {
                print("Открываю...");
            } else {
                print("Не удалось открыть папку конфигов.");
            }
            return SINGLE_SUCCESS;
        }));
    }
}

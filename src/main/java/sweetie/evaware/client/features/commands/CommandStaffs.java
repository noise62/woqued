package sweetie.evaware.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import sweetie.evaware.api.command.Command;
import sweetie.evaware.api.command.CommandRegister;
import sweetie.evaware.api.system.configs.StaffManager;
import sweetie.evaware.client.features.commands.arguments.AnyNameArgument;
import sweetie.evaware.client.features.commands.arguments.StrictlyStaffArgument;

@CommandRegister(name = "staffs")
public class CommandStaffs extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("clear").executes(context -> {
            if (!StaffManager.getInstance().getData().isEmpty()) {
                StaffManager.getInstance().clear();
                print("Список лохов очищен.");
            } else {
                print("Список лохов пуст.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(context -> {
            if (StaffManager.getInstance().getData().isEmpty()) {
                print("Список лохов пуст.");
            } else {
                try {
                    String staffs = String.join(", ", StaffManager.getInstance().getData());
                    print("Лохи: " + staffs);
                } catch (Exception e) {
                    print("Произошла ошибка при получении списка лохов!");
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(argument("player", AnyNameArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (StaffManager.getInstance().contains(nickname)) {
                print("Уже есть в списке лохов!");
            } else {
                StaffManager.getInstance().add(nickname);
                print(nickname + " добавлен в список лохов.");
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("remove").then(argument("player", StrictlyStaffArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (!StaffManager.getInstance().contains(nickname)) {
                print("Нет такого " + nickname + "!");
            } else {
                StaffManager.getInstance().remove(nickname);
                print(nickname + " удален из списка лохов.");
            }
            return SINGLE_SUCCESS;
        })));
    }
}


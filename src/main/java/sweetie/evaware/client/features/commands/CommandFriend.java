package sweetie.evaware.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import sweetie.evaware.api.command.Command;
import sweetie.evaware.api.command.CommandRegister;
import sweetie.evaware.api.system.configs.FriendManager;
import sweetie.evaware.client.features.commands.arguments.AnyNameArgument;
import sweetie.evaware.client.features.commands.arguments.StrictlyFriendArgument;

@CommandRegister(name = "friend")
public class CommandFriend extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("clear").executes(context -> {
            if (!FriendManager.getInstance().getData().isEmpty()) {
                FriendManager.getInstance().clear();
                print("Список друзей очищен.");
            } else {
                print("У тебя нет друзей.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(context -> {
            if (FriendManager.getInstance().getData().isEmpty()) {
                print("У тебя нет друзей.");
            } else {
                try {
                    String friends = String.join(", ", FriendManager.getInstance().getData());
                    print("Друзья: " + friends);
                } catch (Exception e) {
                    print("Произошла ошибка при получении списка друзей.");
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(argument("player", AnyNameArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (FriendManager.getInstance().contains(nickname)) {
                print(nickname + "уже есть в друзьях.");
            } else {
                FriendManager.getInstance().add(nickname);
                print(nickname + " добавлен в друзья.");
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("remove").then(argument("player", StrictlyFriendArgument.create()).executes(context -> {
            String nickname = context.getArgument("player", String.class);

            if (!FriendManager.getInstance().contains(nickname)) {
                print("Вы еще не дружили с " + nickname + ".");
            } else {
                FriendManager.getInstance().remove(nickname);
                print(nickname + " удален из друзей.");
            }
            return SINGLE_SUCCESS;
        })));
    }
}


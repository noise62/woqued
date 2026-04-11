package sweetie.evaware.client.features.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.command.CommandSource;
import sweetie.evaware.api.auth.ProfileRepository;
import sweetie.evaware.api.auth.UUIDUtils;
import sweetie.evaware.api.command.Command;
import sweetie.evaware.api.command.CommandRegister;
import sweetie.evaware.api.system.configs.ConfigSkin;

import java.util.UUID;
import java.util.function.Supplier;

@CommandRegister(name = "skin")
public class CommandSkin extends Command {
    public static Supplier<SkinTextures> customSkinTextures = null;
    public static boolean skinEnabled = false;

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("off").executes(context -> {
            if (!skinEnabled) {
                print("Скин уже сброшен!");
            } else {
                customSkinTextures = null;
                skinEnabled = false;
                ConfigSkin.getInstance().save(null);
                print("Скин успешно сброшен!");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("set").then(argument("name", StringArgumentType.string()).executes(context -> {
            String username = StringArgumentType.getString(context, "name");

            try {
                customSkinTextures = createTextureSupplier(username);
                skinEnabled = true;
                ConfigSkin.getInstance().save(username);
                print("Установлен скин: " + username);

            } catch (Exception e) {
                print("Не удалось установить скин :c");
            }

            return SINGLE_SUCCESS;
        })));
    }

    public static Supplier<SkinTextures> createTextureSupplier(String username) {
        UUID uuid = new ProfileRepository().uuidByName(username);
        if (uuid == null) uuid = UUIDUtils.generateOfflinePlayerUuid(username);

        ProfileResult hui = mc.getSessionService().fetchProfile(uuid, false);
        GameProfile profile = hui == null ? null : hui.profile();
        if (profile == null) profile = new GameProfile(uuid, username);

        return PlayerListEntry.texturesSupplier(profile);
    }

    public static Supplier<SkinTextures> getCustomSkinTextures() {
        return skinEnabled ? customSkinTextures : null;
    }
}

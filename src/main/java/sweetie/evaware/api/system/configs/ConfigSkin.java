package sweetie.evaware.api.system.configs;

import lombok.Getter;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.client.features.commands.CommandSkin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigSkin {
    @Getter private static final ConfigSkin instance = new ConfigSkin();

    private final Path configPath = Paths.get(ClientInfo.CONFIG_PATH_OTHER, "last_skin");;

    private final TimerUtil timerUtil = new TimerUtil();

    public void load() {
        try {
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
                Files.writeString(configPath, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        update();
    }

    public void save(String skinName) {
        try {
            if (skinName != null && !skinName.trim().isEmpty()) {
                Files.writeString(configPath, skinName.trim());
            } else {
                Files.writeString(configPath, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String update() {
        try {
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);

                boolean em = content.isEmpty();
                if (!em) CommandSkin.skinEnabled = true;

                return em ? null : content;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void fetchSkin() {
        if (CommandSkin.skinEnabled && timerUtil.finished(2000)) {
            CommandSkin.customSkinTextures = CommandSkin.createTextureSupplier(update());
            timerUtil.reset();
        }
    }
}

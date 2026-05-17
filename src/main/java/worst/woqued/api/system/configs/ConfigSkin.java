package worst.woqued.api.system.configs;

import lombok.Getter;
import worst.woqued.api.system.backend.ClientInfo;
import worst.woqued.api.utils.math.TimerUtil;
import worst.woqued.client.features.commands.CommandSkin;

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

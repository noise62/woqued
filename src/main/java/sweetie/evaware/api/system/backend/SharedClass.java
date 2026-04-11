package sweetie.evaware.api.system.backend;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import sweetie.evaware.api.system.interfaces.QuickImports;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@UtilityClass
public class SharedClass implements QuickImports {
    public ClientPlayerEntity player() {
        return MinecraftClient.getInstance().player;
    }

    public ClientWorld world() {
        return MinecraftClient.getInstance().world;
    }

    public boolean inPvp() {
        if (mc == null || mc.inGameHud == null) return false;

        BossBarHud bossOverlayGui = mc.inGameHud.getBossBarHud();
        Map<UUID, ClientBossBar> bossBars = bossOverlayGui.bossBars;

        for (ClientBossBar bossInfo : bossBars.values()) {
            String nameStrLower = bossInfo.getName().getString().toLowerCase(Locale.ROOT);
            if (nameStrLower.contains("pvp") || nameStrLower.contains("пвп")) {
                return true;
            }
        }
        return false;
    }

    public boolean openFolder(String path) {
        String os = getOSName().toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec("explorer \"" + path + "\"");
                return true;
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", path});
                return true;
            } else if (os.contains("lin")) {
                String[] fileManagers = {
                        "thunar",   // XFCE
                        "dolphin",  // KDE
                        "nautilus", // GNOME
                        "nemo",     // Cinnamon
                        "pcmanfm",  // LXDE
                        "caja",     // MATE
                        "konqueror" // KDE alternative
                };

                for (String manager : fileManagers) {
                    try {
                        Process p = Runtime.getRuntime().exec(new String[]{manager, path});
                        if (p.isAlive()) return true;
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getOSName() {
        if (System.getProperty("java.vendor").toLowerCase().contains("android") || System.getProperty("java.vm.vendor").toLowerCase().contains("android")) {
            return "Android";
        }

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) return "Windows";
        if (osName.contains("mac")) return "MacOS";
        if (osName.contains("lin")) return "Linux";
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return "Linux/Unix";
        }
        return "Unknown";
    }
}

package sweetie.evaware.api.system.configs;

import lombok.Getter;
import lombok.Value;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.interfaces.QuickImports;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MacroManager implements QuickImports {
    @Getter private static final MacroManager instance = new MacroManager();

    private final File file = new File(ClientInfo.CONFIG_PATH_OTHER + "/macros.json");
    private final List<Macro> macros = new ArrayList<>();

    public void load() {
        ConfigMacros.getInstance().load(file, macros);
    }

    public void save() {
        ConfigMacros.getInstance().save(file, macros);
    }

    public void add(String name, String message, int key) {
        macros.add(new Macro(name, message, key));
        save();
    }

    public void remove(String name) {
        macros.removeIf(m -> m.getName().equalsIgnoreCase(name));
        save();
    }

    public boolean has(String name) {
        return macros.stream().anyMatch(m -> m.getName().equalsIgnoreCase(name));
    }

    public void clear() {
        macros.clear();
        save();
    }

    public void onKeyPressed(int key) {
        if (mc.player == null) return;
        macros.stream()
                .filter(m -> m.getKey() == key)
                .findFirst()
                .ifPresent(m -> {
                    if (m.message.startsWith("/")) {
                        mc.player.networkHandler.sendChatCommand(m.message.replace("/", ""));
                    } else {
                        mc.player.networkHandler.sendChatMessage(m.message);
                    }
                });
    }

    @Value
    public static class Macro {
        String name;
        String message;
        int key;
    }
}
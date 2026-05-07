package worst.woqued.api.system.configs;

import lombok.Getter;
import lombok.Value;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleManager;
import worst.woqued.api.system.backend.ClientInfo;
import worst.woqued.api.system.interfaces.QuickImports;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BindManager implements QuickImports {
    @Getter private static final BindManager instance = new BindManager();

    private final File file = new File(ClientInfo.CONFIG_PATH_OTHER + "/binds.json");
    private final List<Bind> binds = new ArrayList<>();

    public void load() {
        ConfigBinds.getInstance().load(file, binds);
    }

    public void save() {
        ConfigBinds.getInstance().save(file, binds);
    }

    public void add(String feature, int key) {
        binds.removeIf(b -> b.getFeature().equalsIgnoreCase(feature));
        binds.add(new Bind(feature, key));
        save();
    }

    public void remove(String feature) {
        binds.removeIf(b -> b.getFeature().equalsIgnoreCase(feature));
        save();
    }

    public void onKeyPressed(int key) {
        if (mc.player == null) return;

        binds.stream()
                .filter(b -> b.getKey() == key)
                .findFirst()
                .ifPresent(b -> {
                    Module module = ModuleManager.getInstance().getModules().stream()
                            .filter(m -> m.getName().equalsIgnoreCase(b.getFeature()))
                            .findFirst()
                            .orElse(null);
                    if (module != null) {
                        module.toggle();
                    }
                });
    }

    public boolean has(String feature) {
        return binds.stream().anyMatch(b -> b.getFeature().equalsIgnoreCase(feature));
    }

    public int getBind(String feature) {
        return binds.stream()
                .filter(b -> b.getFeature().equalsIgnoreCase(feature))
                .findFirst()
                .map(Bind::getKey)
                .orElse(-999);
    }

    @Value
    public static class Bind {
        String feature;
        int key;
    }
}
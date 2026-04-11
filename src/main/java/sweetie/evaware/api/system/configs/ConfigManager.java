package sweetie.evaware.api.system.configs;

import com.google.gson.*;
import lombok.Getter;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.module.setting.*;
import sweetie.evaware.api.system.backend.ClientInfo;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    @Getter private static final ConfigManager instance = new ConfigManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path CONFIG_DIR;

    private ConfigManager() {
        this.CONFIG_DIR = Paths.get(ClientInfo.CONFIG_PATH_MAIN);
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config dir", e);
        }
    }

    public List<String> getConfigsNames() {
        File configDir = new File(ClientInfo.CONFIG_PATH_MAIN);
        if (!configDir.exists()) return Collections.emptyList();

        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return Collections.emptyList();

        return Arrays.stream(files).map(f -> f.getName().replace(".json", "")).collect(Collectors.toList());
    }

    private Path getConfigFile(String name) {
        return CONFIG_DIR.resolve(name + ".json");
    }

    public void save(String name) {
        JsonObject root = new JsonObject();
        List<Module> modules = ModuleManager.getInstance().getModules();

        JsonObject modulesJson = new JsonObject();
        for (Module m : modules) {
            modulesJson.add(m.getName(), createConfigFromModule(m));
        }

        root.add("Modules", modulesJson);

        Path file = getConfigFile(name);
        try {
            if (!Files.exists(file)) Files.createFile(file);
            Files.writeString(file, GSON.toJson(root));
        } catch (IOException e) {
            System.err.printf("Failed to save config %s: %s%n", name, e.getMessage());
        }
    }

    public void load(String name) {
        Path file = getConfigFile(name);
        if (!Files.exists(file)) return;

        try {
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (!root.has("Modules")) return;

            JsonObject modulesJson = root.getAsJsonObject("Modules");
            modulesJson.entrySet().forEach(entry -> {
                Module module = ModuleManager.getInstance()
                        .getModules().stream()
                        .filter(m -> m.getName().equalsIgnoreCase(entry.getKey()))
                        .findFirst().orElse(null);

                if (module == null) {
                    System.err.println("Module not found for config: " + entry.getKey());
                } else {
                    applyConfigToModule(module, entry.getValue().getAsJsonObject());
                }
            });
        } catch (IOException e) {
            System.err.printf("Failed to load config %s: %s%n", name, e.getMessage());
        }
    }

    public void remove(String name) {
        try {
            Files.deleteIfExists(getConfigFile(name));
        } catch (IOException e) {
            System.err.printf("Failed to remove config %s: %s%n", name, e.getMessage());
        }
    }

    public boolean exists(String name) {
        return Files.exists(getConfigFile(name));
    }

    private JsonObject createConfigFromModule(Module module) {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", module.isEnabled());
        json.addProperty("bind", module.getBind());

        JsonObject settings = new JsonObject();
        module.getSettings().forEach(s -> {
            JsonElement v = serializeSetting(s);
            if (v != null) settings.add(s.getName(), v);
        });
        json.add("settings", settings);
        return json;
    }

    private void applyConfigToModule(Module module, JsonObject json) {
        if (json.has("enabled")) module.setEnabled(json.get("enabled").getAsBoolean(), true);
        if (json.has("bind")) module.setBind(json.get("bind").getAsInt());

        if (json.has("settings")) {
            JsonObject settings = json.getAsJsonObject("settings");
            module.getSettings().forEach(s -> {
                if (settings.has(s.getName())) deserializeSetting(s, settings.get(s.getName()));
            });
        }
    }

    private JsonElement serializeSetting(Setting<?> s) {
        return switch (s) {
            case BooleanSetting b -> GSON.toJsonTree(b.getValue());
            case ModeSetting m -> GSON.toJsonTree(m.getValue());
            case SliderSetting sl -> GSON.toJsonTree(sl.getValue());
            case BindSetting bi -> GSON.toJsonTree(bi.getValue());
            case MultiBooleanSetting mb -> {
                JsonObject obj = new JsonObject();
                mb.getValue().forEach(b -> obj.addProperty(b.getName(), b.getValue()));
                yield obj;
            }
            case ColorSetting c -> {
                Color col = c.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("r", col.getRed());
                obj.addProperty("g", col.getGreen());
                obj.addProperty("b", col.getBlue());
                obj.addProperty("a", col.getAlpha());
                yield obj;
            }
            default -> null;
        };
    }

    private void deserializeSetting(Setting<?> s, JsonElement e) {
        try {
            switch (s) {
                case BooleanSetting b -> b.setValue(e.getAsBoolean());
                case ModeSetting m -> m.setValue(e.getAsString());
                case SliderSetting sl -> sl.setValue(e.getAsFloat());
                case BindSetting bi -> bi.setValue(e.getAsInt());
                case MultiBooleanSetting mb -> {
                    JsonObject obj = e.getAsJsonObject();
                    mb.getValue().forEach(b -> {
                        if (obj.has(b.getName())) b.setValue(obj.get(b.getName()).getAsBoolean());
                    });
                }
                case ColorSetting c -> {
                    JsonObject obj = e.getAsJsonObject();
                    int r = obj.get("r").getAsInt();
                    int g = obj.get("g").getAsInt();
                    int b = obj.get("b").getAsInt();
                    int a = obj.get("a").getAsInt();
                    c.setValue(new Color(r, g, b, a));
                }
                default -> {}
            }
        } catch (Exception ex) {
            System.err.printf("Failed to deserialize %s: %s%n", s.getName(), ex.getMessage());
        }
    }
}
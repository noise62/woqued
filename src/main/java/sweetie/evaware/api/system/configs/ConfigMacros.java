package sweetie.evaware.api.system.configs;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.List;

public class ConfigMacros {
    @Getter private static final ConfigMacros instance = new ConfigMacros();

    private final GsonBuilder gson = new GsonBuilder().setPrettyPrinting();

    public void load(File file, List<MacroManager.Macro> macros) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<MacroManager.Macro>>() {}.getType();
            List<MacroManager.Macro> loaded = gson.create().fromJson(reader, type);
            macros.clear();
            if (loaded != null) macros.addAll(loaded);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void save(File file, List<MacroManager.Macro> macros) {
        File parentDir = file.getParentFile();
        if (parentDir != null) parentDir.mkdirs();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.create().toJson(macros, writer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

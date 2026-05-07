package worst.woqued.api.system.configs;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.List;

public class ConfigBinds {
    @Getter private static final ConfigBinds instance = new ConfigBinds();

    private final GsonBuilder gson = new GsonBuilder().setPrettyPrinting();

    public void load(File file, List<BindManager.Bind> binds) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<BindManager.Bind>>() {}.getType();
            List<BindManager.Bind> loaded = gson.create().fromJson(reader, type);
            binds.clear();
            if (loaded != null) binds.addAll(loaded);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void save(File file, List<BindManager.Bind> binds) {
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
            gson.create().toJson(binds, writer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
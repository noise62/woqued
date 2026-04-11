package sweetie.evaware.api.system.files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import sweetie.evaware.api.system.backend.ClientInfo;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFile {
    private List<String> data = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public abstract String fileName();

    private Path getFilePath() {
        return Path.of(ClientInfo.CONFIG_PATH_OTHER, fileName() + ".json");
    }

    public void save() {
        try {
            Files.createDirectories(Path.of(ClientInfo.CONFIG_PATH_OTHER));
            try (FileWriter writer = new FileWriter(getFilePath().toFile())) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save: " + getFilePath(), e);
        }
    }

    public void load() {
        try {
            Files.createDirectories(Path.of(ClientInfo.CONFIG_PATH_OTHER));
            File file = getFilePath().toFile();

            if (!file.exists()) {
                file.createNewFile();
                save();
                return;
            }

            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                data = GSON.fromJson(reader, listType);
                if (data == null) data = new ArrayList<>();
            }
        } catch (IOException e) {
            data = new ArrayList<>();
        }
    }

    public void add(String value) {
        if (value != null && !value.trim().isEmpty() && !data.contains(value)) {
            data.add(value);
            save();
        }
    }

    public boolean remove(String value) {
        boolean b = data.remove(value);
        if (b) save();
        return b;
    }

    public List<String> getData() {
        return new ArrayList<>(data);
    }

    public void clear() {
        data.clear();
        save();
    }

    public boolean contains(String value) {
        return data.contains(value);
    }

    public int size() {
        return data.size();
    }
}
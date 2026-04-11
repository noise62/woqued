package sweetie.evaware.api.system.draggable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.system.backend.ClientInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class DraggableManager {
    @Getter private static final DraggableManager instance = new DraggableManager();

    @Getter private final LinkedHashMap<String, Draggable> draggables = new LinkedHashMap<>();

    private final File CONFIG_DIR = new File(ClientInfo.CONFIG_PATH_OTHER + "/drags.json");
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    public Draggable create(Module module, String name, float x, float y) {
        draggables.put(name, new Draggable(module, name, x, y));
        return draggables.get(name);
    }

    public void save() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.getParentFile().mkdirs();
        }
        if (CONFIG_DIR.toPath().getFileSystem().isOpen()) {
            try {
                Files.writeString(CONFIG_DIR.toPath(), GSON.toJson(draggables));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.err.println("File system closed. Could not save drag data.");
        }
    }

    public void load() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.getParentFile().mkdirs();
            return;
        }

        try {
            String json = Files.readString(CONFIG_DIR.toPath());
            Map<String, Draggable> loadedDraggables = GSON.fromJson(json, new TypeToken<Map<String, Draggable>>() {}.getType());

            if (loadedDraggables != null) {
                for (Map.Entry<String, Draggable> entry : loadedDraggables.entrySet()) {
                    String name = entry.getKey();
                    Draggable draggable = entry.getValue();
                    if (draggable != null) {
                        Draggable currentDraggable = draggables.get(name);
                        if (currentDraggable != null) {
                            currentDraggable.setX(draggable.getX());
                            currentDraggable.setY(draggable.getY());
                            draggables.put(name, currentDraggable);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

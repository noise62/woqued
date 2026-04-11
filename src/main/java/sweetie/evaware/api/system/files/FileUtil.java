package sweetie.evaware.api.system.files;

import com.google.gson.Gson;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.interfaces.QuickImports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@UtilityClass
public class FileUtil implements QuickImports {
    private final Gson GSON = new Gson();

    private static final String ASSET_ID = "evaware";

    public InputStream getFromAssets(String input) {
        return FileUtil.class.getResourceAsStream("/assets/" + ASSET_ID + "/" + input);
    }

    public Identifier getImage(String path) {
        return Identifier.of(ASSET_ID, "images/" + path + ".png");
    }

    public Identifier getShader(String name) {
        return Identifier.of(ASSET_ID, "core/" + name);
    }

    public <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
        return GSON.fromJson(toString(identifier), clazz);
    }

    public String toString(Identifier identifier) {
        return toString(identifier, "\n");
    }

    public String toString(Identifier identifier, String delimiter) {
        try(InputStream inputStream = mc.getResourceManager().open(identifier);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining(delimiter));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

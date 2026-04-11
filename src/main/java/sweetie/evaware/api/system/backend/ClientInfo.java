package sweetie.evaware.api.system.backend;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

@UtilityClass
public class ClientInfo {
    public final String NAME = "Woqued";
    public final String VERSION = FabricLoader.getInstance().getModContainer("evaware").get().getMetadata().getVersion().getFriendlyString();

    public final String GAME_PATH = new File(System.getProperty("user.dir")).getAbsolutePath();
    public final String CONFIG_PATH_AI_MODELS = new File(System.getProperty("user.dir"), NAME + "/ai_models").getAbsolutePath();
    public final String CONFIG_PATH_OTHER = new File(System.getProperty("user.dir"), NAME + "/other").getAbsolutePath();
    public final String CONFIG_PATH_THEMES = new File(System.getProperty("user.dir"), NAME + "/themes").getAbsolutePath();
    public final String CONFIG_PATH_MAIN = new File(System.getProperty("user.dir"), NAME + "/main").getAbsolutePath();
}

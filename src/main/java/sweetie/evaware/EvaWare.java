package sweetie.evaware;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import sweetie.evaware.api.command.CommandManager;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.system.DiscordHook;
import sweetie.evaware.api.system.configs.ConfigManager;
import sweetie.evaware.api.system.configs.ConfigSkin;
import sweetie.evaware.api.system.configs.FriendManager;
import sweetie.evaware.api.system.configs.MacroManager;
import sweetie.evaware.api.system.draggable.DraggableManager;
import sweetie.evaware.api.system.files.FileManager;
import sweetie.evaware.api.utils.other.SoundUtil;
import sweetie.evaware.api.utils.render.KawaseBlurProgram;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.client.services.HeartbeatService;
import sweetie.evaware.client.services.RenderService;
import sweetie.evaware.client.ui.theme.ThemeEditor;
import sweetie.evaware.client.ui.widget.WidgetManager;

public class EvaWare implements ClientModInitializer {
	@Getter private static EvaWare instance = new EvaWare();

    @Override
	public void onInitializeClient() {
        instance = this;

        SoundUtil.load();

        loadManagers();
        loadServices();
        loadFiles();
    }

    public void postLoad() {
        ModuleManager.getInstance().getModules().sort((a, b) -> Float.compare(
                Fonts.PS_MEDIUM.getWidth(b.getName(), 7f),
                Fonts.PS_MEDIUM.getWidth(a.getName(), 7f)
        ));

        KawaseBlurProgram.load();
    }

    private void loadFiles() {
        ConfigManager.getInstance().load("autoConfig");
        DraggableManager.getInstance().load();
        FriendManager.getInstance().load();
        MacroManager.getInstance().load();
    }

    private void loadManagers() {
        WidgetManager.getInstance().load();
        RotationManager.getInstance().load();

        ModuleManager.getInstance().load();
        CommandManager.getInstance().load();

        ThemeEditor.getInstance().load();
    }

    private void loadServices() {
        HeartbeatService.getInstance().load();
        RenderService.getInstance().load();
        ConfigSkin.getInstance().load();

        DiscordHook.startRPC();
    }

    public void onClose() {
        ConfigManager.getInstance().save("autoConfig");
        FileManager.getInstance().save();
        ThemeEditor.getInstance().save(true);
        DraggableManager.getInstance().save();
        MacroManager.getInstance().save();

        DiscordHook.stopRPC();
    }
}
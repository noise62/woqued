package worst.woqued;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import worst.woqued.api.command.CommandManager;
import worst.woqued.api.module.ModuleManager;
import worst.woqued.api.system.DiscordHook;
import worst.woqued.api.system.configs.ConfigManager;
import worst.woqued.api.system.configs.ConfigSkin;
import worst.woqued.api.system.configs.FriendManager;
import worst.woqued.api.system.configs.MacroManager;
import worst.woqued.api.system.draggable.DraggableManager;
import worst.woqued.api.system.files.FileManager;
import worst.woqued.api.utils.other.SoundUtil;
import worst.woqued.api.utils.render.KawaseBlurProgram;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.client.services.HeartbeatService;
import worst.woqued.client.services.RenderService;
import worst.woqued.client.ui.theme.ThemeEditor;
import worst.woqued.client.ui.widget.WidgetManager;

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
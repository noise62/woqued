package worst.woqued.client.features.modules.movement.nitrofirework;

import lombok.Getter;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.system.backend.Choice;
import worst.woqued.client.features.modules.movement.nitrofirework.modes.*;
import worst.woqued.client.features.modules.movement.nitrofirework.modes.NitroFireworkCustom;
import worst.woqued.client.features.modules.movement.nitrofirework.modes.NitroFireworkLG;

@ModuleRegister(name = "Nitro Firework", category = Category.MOVEMENT)
public class NitroFireworkModule extends Module {
    @Getter private static final NitroFireworkModule instance = new NitroFireworkModule();

    private final NitroFireworkCustom nitroFireworkCustom = new NitroFireworkCustom(() -> getMode().is("Custom"));
    private final NitroFireworkLG nitroFireworkLG = new NitroFireworkLG(() -> getMode().is("Grim"));

    private final NitroFireworkMode[] modes = new NitroFireworkMode[]{
            nitroFireworkCustom, nitroFireworkLG
    };

    public NitroFireworkMode currentMode = nitroFireworkCustom;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Custom").values(
            Choice.getValues(modes)
    ).onAction(() -> {
        currentMode = (NitroFireworkMode) Choice.getChoiceByName(getMode().getValue(), modes);
    });

    public NitroFireworkModule() {
        addSettings(mode);
        getSettings().addAll(nitroFireworkCustom.getSettings());
        getSettings().addAll(nitroFireworkLG.getSettings());
    }

    @Override
    public void onEvent() {

    }
}

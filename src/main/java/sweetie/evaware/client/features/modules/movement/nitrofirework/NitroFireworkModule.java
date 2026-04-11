package sweetie.evaware.client.features.modules.movement.nitrofirework;

import lombok.Getter;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.client.features.modules.movement.nitrofirework.modes.*;

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

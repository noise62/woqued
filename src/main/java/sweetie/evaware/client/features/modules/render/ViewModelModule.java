package sweetie.evaware.client.features.modules.render;

import lombok.Getter;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.module.setting.RunSetting;

@ModuleRegister(name = "View Model", category = Category.RENDER)
public class ViewModelModule extends Module {
    @Getter private static final ViewModelModule instance = new ViewModelModule();

    public final SliderSetting rightX = new SliderSetting("Right X").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting rightY = new SliderSetting("Right Y").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting rightZ = new SliderSetting("Right Z").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftX = new SliderSetting("Left X").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftY = new SliderSetting("Left Y").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftZ = new SliderSetting("Left Z").value(0f).range(-2f, 2f).step(0.1f);

    private final RunSetting reset = new RunSetting("Reset position").value(this::resetPos);

    public ViewModelModule() {
        addSettings(rightX, rightY, rightZ, leftX, leftY, leftZ, reset);
    }

    @Override
    public void onEvent() {

    }

    private void resetPos() {
        getSettings().forEach(setting -> {
            if (setting instanceof SliderSetting f) {
                f.setValue(0f);
            }
        });
    }
}

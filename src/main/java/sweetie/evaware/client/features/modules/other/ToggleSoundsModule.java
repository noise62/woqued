package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.other.SoundUtil;

@ModuleRegister(name = "Toggle Sounds", category = Category.OTHER)
public class ToggleSoundsModule extends Module {
    @Getter private static final ToggleSoundsModule instance = new ToggleSoundsModule();

    private final ModeSetting sound = new ModeSetting("Sound").value("Blop").values("Smooth", "Celestial", "Nursultan", "Akrien", "Tech", "Blop");
    public final SliderSetting volume = new SliderSetting("Volume").value(60f).range(1f, 100f).step(1f);

    public ToggleSoundsModule() {
        addSettings(sound, volume);
    }

    public static void playToggle(boolean state) {
        if (!instance.isEnabled()) return;

        SoundUtil.playSound(switch (instance.sound.getValue()) {
            case "Nursultan" -> state ? SoundUtil.ENABLE_NU_EVENT : SoundUtil.DISABLE_NU_EVENT;
            case "Celestial" -> state ? SoundUtil.ENABLE_CEL_EVENT : SoundUtil.DISABLE_CEL_EVENT;
            case "Akrien" -> state ? SoundUtil.ENABLE_AK_EVENT : SoundUtil.DISABLE_AK_EVENT;
            case "Tech" -> state ? SoundUtil.ENABLE_TECH_EVENT : SoundUtil.DISABLE_TECH_EVENT;
            case "Blop" -> state ? SoundUtil.ENABLE_BLOP_EVENT : SoundUtil.DISABLE_BLOP_EVENT;
            default -> state ? SoundUtil.ENABLE_SMOOTH_EVENT : SoundUtil.DISABLE_SMOOTH_EVENT;
        });
    }
    @Override
    public void onEvent() {

    }
}

package worst.woqued.client.features.modules.movement;

import lombok.Getter;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationPlan;
import worst.woqued.client.features.modules.combat.AuraModule;

@ModuleRegister(name = "Move Fix", category = Category.MOVEMENT)
public class MoveFixModule extends Module {
    @Getter private static final MoveFixModule instance = new MoveFixModule();

    private final ModeSetting mode = new ModeSetting("Mode").value("Focus").values("Focus", "Free");
    public final BooleanSetting targeting = new BooleanSetting("Targeting").value(true);

    public MoveFixModule() {
        addSettings(mode, targeting);
    }

    @Override
    public void onEvent() {

    }

    public static boolean isTargeting() {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan plan = rotationManager.getCurrentRotationPlan();
        return plan != null && plan.provider() instanceof AuraModule && instance.targeting.getValue();
    }

    public static boolean enabled() {
        return instance.isEnabled();
    }

    public static boolean isFree() {
        return instance.mode.is("Free");
    }
}

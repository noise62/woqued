package sweetie.evaware.client.features.modules.movement;

import lombok.Getter;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;
import sweetie.evaware.client.features.modules.combat.AuraModule;

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

package worst.woqued.client.features.modules.render;

import lombok.Getter;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;

@ModuleRegister(name = "Smooth Camera", category = Category.RENDER)
public class SmoothCameraModule extends Module {
    @Getter private static final SmoothCameraModule instance = new SmoothCameraModule();

    private final SliderSetting factorH = new SliderSetting("Horizontal Factor").value(0.9f).range(0f, 1f).step(0.01f);
    private final SliderSetting factorV = new SliderSetting("Vertical Factor").value(0.93f).range(0f, 1f).step(0.01f);
    private final BooleanSetting resetOnPerspectiveChange = new BooleanSetting("Reset On Perspective Change").value(true);
    private final BooleanSetting enableFirstPOV = new BooleanSetting("Enable First POV").value(false);

    @Getter private Vec3d smoothPos = Vec3d.ZERO;
    private Object lastPerspective = null;

    public SmoothCameraModule() {
        addSettings(factorH, factorV, resetOnPerspectiveChange, enableFirstPOV);
    }

    @Override
    public void onDisable() {
        smoothPos = Vec3d.ZERO;
        lastPerspective = null;
    }

    @Override
    public void onEvent() {}

    public void updateCamera(Vec3d pos) {
        if (!isEnabled()) {
            lastPerspective = getPerspective();
            return;
        }

        Object currentPerspective = getPerspective();

        if (resetOnPerspectiveChange.getValue() && lastPerspective != null && !lastPerspective.equals(currentPerspective)) {
            smoothPos = pos;
            lastPerspective = currentPerspective;
            return;
        }

        lastPerspective = currentPerspective;

        if (!enableFirstPOV.getValue() && mc.options.getPerspective().isFirstPerson()) {
            smoothPos = pos;
            return;
        }

        if (smoothPos.equals(Vec3d.ZERO)) {
            smoothPos = pos;
            return;
        }

        float h = factorH.getValue();
        float v = factorV.getValue();

        smoothPos = new Vec3d(
                smoothPos.x * h + pos.x * (1 - h),
                smoothPos.y * v + pos.y * (1 - v),
                smoothPos.z * h + pos.z * (1 - h)
        );
    }

    private Object getPerspective() {
        return mc.options.getPerspective();
    }
}
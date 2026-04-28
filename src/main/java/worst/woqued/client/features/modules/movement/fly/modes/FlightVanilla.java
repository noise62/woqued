package worst.woqued.client.features.modules.movement.fly.modes;

import worst.woqued.api.module.setting.Setting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.client.features.modules.movement.fly.FlightMode;

import java.util.function.Supplier;

public class FlightVanilla extends FlightMode {
    @Override
    public String getName() {
        return "Vanilla";
    }

    private final SliderSetting speedH = new SliderSetting("Speed H").value(1f).range(0.1f, 5f).step(0.1f);
    private final SliderSetting speedV = new SliderSetting("Speed V").value(1f).range(0.1f, 5f).step(0.1f);

    public FlightVanilla(Supplier<Boolean> condition) {
        addSettings(speedH, speedV);

        for (Setting<?> setting : getSettings()) {
            setting.setVisible(condition);
        }
    }

    @Override
    public void onUpdate() {
        MoveUtil.setSpeed(speedH.getValue());

        float y = speedV.getValue();

        if (mc.options.jumpKey.isPressed()) {
            mc.player.getVelocity().y = y;
        } else if (mc.options.sneakKey.isPressed()) {
            mc.player.getVelocity().y = -y;
        } else {
            mc.player.getVelocity().y = 0f;
        }
    }
}

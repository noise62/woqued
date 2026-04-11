package sweetie.evaware.client.features.modules.movement.nitrofirework.modes;

import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.module.setting.Setting;
import sweetie.evaware.api.system.backend.Pair;
import sweetie.evaware.client.features.modules.movement.nitrofirework.NitroFireworkMode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NitroFireworkCustom extends NitroFireworkMode {
    @Override
    public String getName() {
        return "Custom";
    }
    private final float[] angles = {0f, 5f, 15f, 20f, 25f, 30f, 35f, 40f, 45f};

    private final List<SliderSetting> speedSettings = new ArrayList<>();

    public NitroFireworkCustom(Supplier<Boolean> condition) {
        for (float angle : angles) {
            SliderSetting setting = new SliderSetting((int)angle + "° speed").value(1.5f).range(1.5f, 3f).step(0.1f).setVisible(condition);
            addSettings(setting);
            speedSettings.add(setting);
        }
    }

    @Override
    public Pair<Float, Float> velocityValues() {
        int closestIndex = 0;
        float minDiff = Float.MAX_VALUE;

        float playerYaw = ((mc.player.getYaw() % 360) + 360) % 360;

        for (int i = 0; i < angles.length; i++) {
            float angleVal = (angles[i] + 360) % 360;
            float diff = Math.abs(((playerYaw - angleVal + 180) % 360 + 360) % 360 - 180);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }

        float speed = speedSettings.get(closestIndex).getValue();
        return new Pair<>(speed, 1.5f);
    }
}
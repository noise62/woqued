package sweetie.evaware.client.features.modules.movement.spider.modes;

import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.client.features.modules.movement.spider.SpiderMode;

import java.util.function.Supplier;

public class SpiderMatrix extends SpiderMode {
    @Override
    public String getName() {
        return "Matrix";
    }

    private final SliderSetting delay = new SliderSetting("Delay").value(2f).range(1f, 15f).step(1f);

    public SpiderMatrix(Supplier<Boolean> condition) {
        delay.setVisible(condition);
        addSettings(delay);
    }

    @Override
    public void onUpdate() {
        if (!hozColl()) return;

        mc.player.setOnGround(mc.player.age % delay.getValue().intValue() == 0);
        mc.player.prevY -= 2.0E-232;
        if (mc.player.isOnGround())
            mc.player.setVelocity(mc.player.getVelocity().getX(), 0.42, mc.player.getVelocity().getZ());
    }
}

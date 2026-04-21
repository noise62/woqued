package worst.woqued.client.features.modules.movement.noslow.modes;

import worst.woqued.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowReallyWorld extends NoSlowMode {
    private int ticks = 0;

    @Override
    public String getName() {
        return "Really World";
    }

    @Override
    public void onUpdate() {
        if (mc.player.isUsingItem()) {
            ticks++;
        } else {
            ticks = 0;
        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean slowingCancel() {
        return ticks % 5 == 0;
    }
}

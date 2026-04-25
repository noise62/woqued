package worst.woqued.client.features.modules.movement.noslow.modes;

import worst.woqued.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowReallyWorld extends NoSlowMode {
    private int ticks = 0;
    private int cycleCounter = 0;

    @Override
    public String getName() {
        return "Really World";
    }

    @Override
    public void onUpdate() {
        if (!mc.player.isUsingRiptide()) {
            if (mc.player.isUsingItem()) {
                ticks++;
            } else {
                ticks = 0;
                cycleCounter = 0;
            }
        }
    }

    @Override
    public void onTick() {
    }

    @Override
    public boolean slowingCancel() {
        int[] thresholds;

        if (mc.options.jumpKey.isPressed()) {
            thresholds = new int[]{2, 2, 2};
        } else {
            thresholds = new int[]{2, 3, 3};
        }

        int threshold = thresholds[cycleCounter % thresholds.length];

        if (ticks >= threshold) {
            ticks = 0;
            cycleCounter++;
            return true;
        }

        return false;
    }
}
package worst.woqued.client.features.modules.movement.speed.modes;

import worst.woqued.api.system.client.TimerManager;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;
import worst.woqued.client.features.modules.movement.speed.SpeedModule;

import java.util.function.Supplier;

public class SpeedMatrix692 extends SpeedMode {
    private boolean wasTimer = false;

    public SpeedMatrix692(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "Matrix6.9.2";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        if (mc.player.isInLava() || PlayerUtil.isInWeb() || mc.player.isClimbing()) return;

        if (wasTimer) {
            wasTimer = false;
            TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
        }

        mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.00348, mc.player.getVelocity().z);
        mc.options.jumpKey.setPressed(mc.options.jumpKey.isPressed());

        if (MoveUtil.isMoving() && mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(false);
            TimerManager.getInstance().addTimer(1.35f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
            wasTimer = true;
            mc.player.jump();
            double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            MoveUtil.setSpeed(currentSpeed);
        } else {
            double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            if (currentSpeed < 0.215) {
                MoveUtil.setSpeed(0.215);
            }
        }
    }

    @Override
    public void onDisable() {
        wasTimer = false;
        TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
    }
}
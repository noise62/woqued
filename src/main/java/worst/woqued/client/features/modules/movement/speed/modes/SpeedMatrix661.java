package worst.woqued.client.features.modules.movement.speed.modes;

import worst.woqued.api.system.client.TimerManager;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;
import worst.woqued.client.features.modules.movement.speed.SpeedModule;

import java.util.function.Supplier;

public class SpeedMatrix661 extends SpeedMode {
    public SpeedMatrix661(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "Matrix6.6.1";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        if (mc.player.isInLava() || PlayerUtil.isInWeb() || mc.player.isClimbing()) return;

        if (!mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(mc.options.jumpKey.isPressed());
            double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            if (currentSpeed < 0.217) {
                MoveUtil.setSpeed(0.217);
            }
        }

        if (mc.player.getVelocity().y < 0) {
            TimerManager.getInstance().addTimer(1.09f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
            if (mc.player.fallDistance > 1.4) {
                TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
            }
        } else {
            TimerManager.getInstance().addTimer(0.95f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
        }

        if (mc.player.isOnGround() && MoveUtil.isMoving()) {
            mc.options.jumpKey.setPressed(false);
            TimerManager.getInstance().addTimer(1.03f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
            mc.player.jump();
            if (mc.player.input.movementSideways <= 0.01 && mc.player.input.movementSideways >= -0.01) {
                double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
                MoveUtil.setSpeed(currentSpeed * 1.0071);
            }
        } else if (!MoveUtil.isMoving()) {
            TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
        }

        double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
        if (currentSpeed < 0.22) {
            MoveUtil.setSpeed(currentSpeed);
        }
    }

    @Override
    public void onDisable() {
        TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
    }
}
package worst.woqued.client.features.modules.movement.speed.modes;

import worst.woqued.api.system.client.TimerManager;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;
import worst.woqued.client.features.modules.movement.speed.SpeedModule;

import java.util.function.Supplier;

public class SpeedMatrixSlowHop extends SpeedMode {
    public SpeedMatrixSlowHop(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "MatrixSlowHop";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        if (mc.player.isSubmergedInWater() || mc.player.isInLava() || PlayerUtil.isInWeb() || mc.player.isClimbing()) return;

        if (MoveUtil.isMoving()) {
            if (!mc.player.isOnGround() && mc.player.fallDistance > 2) {
                TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
                return;
            }

            if (mc.player.isOnGround()) {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.42 - 0.00348, mc.player.getVelocity().z);
                TimerManager.getInstance().addTimer(0.5195f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
                double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
                MoveUtil.setSpeed(currentSpeed + 0.02);
            } else {
                TimerManager.getInstance().addTimer(1.0973f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);

                if (mc.player.fallDistance <= 0.4 && mc.player.input.movementSideways == 0f) {
                    double airSpeed = 0.02035f * 1.5;
                    double[] forward = MoveUtil.forward(airSpeed);
                    mc.player.setVelocity(mc.player.getVelocity().x + forward[0], mc.player.getVelocity().y, mc.player.getVelocity().z + forward[1]);
                } else {
                    double airSpeed = 0.02f * 1.5;
                    double[] forward = MoveUtil.forward(airSpeed);
                    mc.player.setVelocity(mc.player.getVelocity().x + forward[0], mc.player.getVelocity().y, mc.player.getVelocity().z + forward[1]);
                }
            }
        } else {
            TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, SpeedModule.getInstance(), 1);
        }
    }
}
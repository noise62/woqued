package worst.woqued.client.features.modules.movement.speed.modes;

import worst.woqued.api.system.client.TimerManager;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;

import java.util.function.Supplier;

public class SpeedMatrixHop2 extends SpeedMode {
    public SpeedMatrixHop2(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "MatrixHop2";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        if (mc.player.isInLava() || PlayerUtil.isInWeb() || mc.player.isClimbing()) return;

        if (MoveUtil.isMoving()) {
            if (mc.player.isOnGround()) {
                mc.options.jumpKey.setPressed(false);
                TimerManager.getInstance().addTimer(1.0f, worst.woqued.api.utils.task.TaskPriority.HIGH, worst.woqued.client.features.modules.movement.speed.SpeedModule.getInstance(), 1);
                mc.player.jump();
            }

            if (mc.player.getVelocity().y > 0.003) {
                mc.player.setVelocity(mc.player.getVelocity().x * 1.0012, mc.player.getVelocity().y, mc.player.getVelocity().z * 1.0012);
                TimerManager.getInstance().addTimer(1.05f, worst.woqued.api.utils.task.TaskPriority.HIGH, worst.woqued.client.features.modules.movement.speed.SpeedModule.getInstance(), 1);
            }
        }
    }
}
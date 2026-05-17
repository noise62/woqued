package worst.woqued.client.features.modules.movement.speed.modes;

import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;

import java.util.function.Supplier;

public class SpeedMatrixHop extends SpeedMode {
    public SpeedMatrixHop(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "MatrixHop";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        if (mc.player.isInLava() || PlayerUtil.isInWeb() || mc.player.isClimbing()) return;

        if (MoveUtil.isMoving()) {
            if (mc.player.isOnGround()) {
                double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
                MoveUtil.setSpeed(currentSpeed + 0.02);
                mc.player.setVelocity(mc.player.getVelocity().x, 0.42 - 0.00348, mc.player.getVelocity().z);
            }

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
    }
}
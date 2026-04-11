package sweetie.evaware.api.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;

@UtilityClass
public class MoveUtil implements QuickImports {
    public KeyBinding[] getMovementKeys() {
        return new KeyBinding[]{
                mc.options.sprintKey,
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey
        };
    }

    public void updateMovementKeys() {
        for (KeyBinding movementKey : getMovementKeys()) {
            movementKey.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), movementKey.getDefaultKey().getCode()));
        }
    }

    public boolean w() {
        return mc.options.forwardKey.isPressed();
    }

    public boolean s() {
        return mc.options.backKey.isPressed();
    }

    public boolean a() {
        return mc.options.leftKey.isPressed();
    }

    public boolean d() {
        return mc.options.rightKey.isPressed();
    }

    public boolean isMoving() {
        return mc.player.forwardSpeed != 0f || mc.player.sidewaysSpeed != 0f;
    }

    public double direction(float rotationYaw, final float moveForward, final float moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;
        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        if (moveForward > 0F) forward = 0.5F;
        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;
        return Math.toRadians(rotationYaw);
    }

    public double[] forward(double speed) {
        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = currentRotationPlan == null ? mc.player.getYaw() : rotation.getYaw();
        if (forward != 0.0f) {
            if (strafe > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (strafe < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            strafe = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        double cosStrafe = Math.sin(Math.toRadians(yaw + 90.0f));
        double sinStrafe = Math.cos(Math.toRadians(yaw + 90.0f));
        double x = forward * speed * sinStrafe + strafe * speed * cosStrafe;
        double z = forward * speed * cosStrafe - strafe * speed * sinStrafe;
        return new double[]{x, z};
    }

    public void setSpeed(double speed) {
        double[] forward = forward(speed);

        mc.player.setVelocity(forward[0], mc.player.getVelocity().y, forward[1]);
    }
}

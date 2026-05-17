package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;

public class ScaffoldRotation extends RotationMode {

    public ScaffoldRotation() {
        super("Scaffold");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        float currentYaw = currentRotation.getYaw();
        float currentPitch = currentRotation.getPitch();
        float targetYaw = targetRotation.getYaw();
        float targetPitch = targetRotation.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.5f) {
            return targetRotation;
        }

        float speed = 0.15f;

        float maxYawStep = Math.min(Math.abs(yawDelta), 25f);
        float maxPitchStep = Math.min(Math.abs(pitchDelta), 20f);

        float newYaw = currentYaw + Math.signum(yawDelta) * maxYawStep * speed;
        float newPitch = currentPitch + Math.signum(pitchDelta) * maxPitchStep * speed;

        return new Rotation(newYaw, newPitch);
    }
}
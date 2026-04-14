package sweetie.evaware.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;

public class MatrixRotation extends RotationMode {
    private static float lastYaw, lastPitch;

    public MatrixRotation() {
        super("Really World");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        // Плавное возвращение камеры когда entity == null (таргет потерян / модуль отключён)
        if (entity == null) {
            float speedFactor = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.05f, 0.4f);
            float speed = 0.35F * speedFactor;

            float lineYaw = rotationDifference > 0 ? (Math.abs(yawDelta / rotationDifference) * 360) : 360;
            float linePitch = rotationDifference > 0 ? (Math.abs(pitchDelta / rotationDifference) * 180) : 180;

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            return new Rotation(
                    MathHelper.lerp(speed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw),
                    MathHelper.lerp(speed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch)
            );
        }

        float yawSpeed = MathUtil.randomInRange(58, 60);
        float pitchSpeed = FunTimeRotation.attack || entity == null ? Math.abs(pitchDelta) : 0.333f;

        float pitchStep = Math.max(pitchDelta, (float) (2.0 + Math.random() * 2.0));
        pitchDelta = (pitchDelta > 0) ? pitchStep : -pitchStep;

        float jitterPower = 3f;
        if (Math.abs(yawDelta - lastYaw) >= jitterPower) {
            float jitterApplier = jitterPower + 0.1f;
            yawDelta += (yawDelta > 0) ? jitterApplier : -jitterApplier;
        }

        lastYaw = yawDelta;
        lastPitch = pitchDelta;

        return new Rotation(
                currentRotation.getYaw() + MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed),
                currentRotation.getPitch() + MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed)
        );
    }
}

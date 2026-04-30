package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AuraModule;

public class PolarRotation extends RotationMode {

    private float jitterYaw;
    private float jitterPitch;

    public PolarRotation() {
        super("Polar");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AuraModule aura = AuraModule.getInstance();
        boolean canAttack = entity != null && aura.combatExecutor.combatManager().canAttack();
        boolean hasTarget = entity != null && aura.target != null;

        if (canAttack && entity != null) {
            float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
            float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

            float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

            if (rotationDifference > 180f) {
                return targetRotation;
            }

            float speed = Math.min(1.0f, rotationDifference / 20f);
            float lerpFactor = 0.5f + speed * 0.5f;

            float newYaw = MathHelper.lerp(lerpFactor, currentRotation.getYaw(), targetRotation.getYaw());
            float newPitch = MathHelper.lerp(lerpFactor, currentRotation.getPitch(), targetRotation.getPitch());

            updateJitter();
            newYaw += jitterYaw;
            newPitch += jitterPitch;

            return new Rotation(newYaw, newPitch);
        }

        if (hasTarget) {
            updateJitter();
        }

        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

        float speed = 0.3f;

        float jitterY = hasTarget ? jitterYaw : 0;
        float jitterP = hasTarget ? jitterPitch : 0;

        return new Rotation(
                currentRotation.getYaw() + yawDelta * speed + jitterY,
                currentRotation.getPitch() + pitchDelta * speed + jitterP
        );
    }

    private void updateJitter() {
        jitterYaw = (float) (Math.random() - 0.5f) * 3.0f;
        jitterPitch = (float) (Math.random() - 0.5f) * 1.5f;
    }
}
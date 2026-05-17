package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AuraModule;

public class SnapRotation extends RotationMode {

    public SnapRotation() {
        super("Snap");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AuraModule aura = AuraModule.getInstance();
        boolean canAttack = entity != null && aura.combatExecutor.combatManager().canAttack();

        if (canAttack) {
            float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
            float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

            float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

            if (rotationDifference > 180f) {
                return targetRotation;
            }

            float speed = Math.min(1.0f, rotationDifference / 15f);
            float lerpFactor = 0.8f + speed * 0.2f;

            return new Rotation(
                    MathHelper.lerp(lerpFactor, currentRotation.getYaw(), targetRotation.getYaw()),
                    MathHelper.lerp(lerpFactor, currentRotation.getPitch(), targetRotation.getPitch())
            );
        }

        Rotation delta = RotationUtil.calculateDelta(currentRotation, RotationUtil.fromVec2f(mc.player.getRotationClient()));
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.1f) {
            return RotationUtil.fromVec2f(mc.player.getRotationClient());
        }

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
}
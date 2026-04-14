package sweetie.evaware.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;
import sweetie.evaware.client.features.modules.combat.AuraModule;

import java.security.SecureRandom;

/**
 * Snap — полный перенос логики SnapAngle из Rich-Modern.
 * Мгновенное наведение с прямой интерполяцией к цели.
 */
public class SnapRotation extends RotationMode {

    private final SecureRandom random = new SecureRandom();

    public SnapRotation() {
        super("Grim");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AuraModule aura = AuraModule.getInstance();

        Rotation angleDelta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        boolean canAttack = entity != null && aura.combatExecutor.combatManager().canAttack();

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

        float preAttackSpeed = 1F;
        float postAttackSpeed = 1F;
        float speed = canAttack ? preAttackSpeed : postAttackSpeed;

        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float newYaw = MathHelper.lerp(speed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw);
        float newPitch = MathHelper.lerp(speed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch);

        return new Rotation(newYaw, newPitch);
    }
}

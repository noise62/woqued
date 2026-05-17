package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AuraModule;

/**
 * Ares Mine — плавная линейная ротация к цели.
 */
public class AresMineRotation extends RotationMode {

    public AresMineRotation() {
        super("Ares Mine");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        boolean canAttack = entity != null && AuraModule.getInstance().getCombatExecutor().combatManager().canAttack();

        Rotation delta = RotationUtil.calculateDelta(currentRotation, canAttack ? targetRotation : RotationUtil.fromVec2f(mc.player.getRotationClient()));
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.1f) {
            return canAttack ? targetRotation : RotationUtil.fromVec2f(mc.player.getRotationClient());
        }

        // Фиксированная плавная скорость вращения
        float speed = canAttack ? 0.5f : 0.35f;

        // Линейная интерполяция к целевому углу
        float smoothYaw = MathHelper.lerp(speed, currentRotation.getYaw(), targetRotation.getYaw());
        float smoothPitch = MathHelper.clamp(
                MathHelper.lerp(speed, currentRotation.getPitch(), targetRotation.getPitch()),
                -89.0f, 89.0f
        );

        return new Rotation(smoothYaw, smoothPitch);
    }
}

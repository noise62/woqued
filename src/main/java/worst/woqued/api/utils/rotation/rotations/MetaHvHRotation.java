package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;

/**
 * MetaHvH — супер быстрая ротация с мгновенной интерполяцией.
 * Мгновенно поворачивает к целевому углу.
 */
public class MetaHvHRotation extends RotationMode {
    private static final float LERP_SPEED = 1.0f; // Мгновенная ротация

    public MetaHvHRotation() {
        super("MetaHvH");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        // Линейная интерполяция (LERP)
        float newYaw = MathHelper.lerp(LERP_SPEED, currentRotation.getYaw(), currentRotation.getYaw() + yawDelta);
        float newPitch = MathHelper.lerp(LERP_SPEED, currentRotation.getPitch(), currentRotation.getPitch() + pitchDelta);

        // Ограничение питча допустимыми значениями
        newPitch = MathHelper.clamp(newPitch, -89.0f, 89.0f);

        return new Rotation(newYaw, newPitch);
    }
}

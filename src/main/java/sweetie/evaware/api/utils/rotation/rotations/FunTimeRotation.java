package sweetie.evaware.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.player.MoveUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;

// kurwa code
public class FunTimeRotation extends RotationMode {
    public static boolean attack;

    public static int attackedTicks = 0;
    public static int attackCount = 0;

    public static int idAttack = 0;

    public static long lastAttack = 0;
    public static float jitterUniquePower = 0f;

    public static boolean firstAttack = true;

    private boolean releasing = false;

    public FunTimeRotation() {
        super("FunTime");
    }

    /**
     * Плавное возвращение теперь обрабатывается в process() при entity == null.
     * Оставлен для обратной совместимости.
     */
    public void startRelease() {
        releasing = true;
    }

    public boolean isReleasing() {
        return releasing;
    }

    public static void updateAttackState(boolean attack) {
        FunTimeRotation.attack = attack;

        if (attack) {
            attackCount++;
            if (firstAttack && attackCount >= 2) {
                firstAttack = false;
                attackCount = 0;
            }

            jitterUniquePower = MathUtil.randomInRange(5f, 15f);

            lastAttack = System.currentTimeMillis();

            idAttack = MathUtil.randomInRange(0, 1);

            attackedTicks = MathUtil.randomInRange(12, 16) * 3;
        }
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (entity == null) {
            // Плавное возвращение камеры к камере игрока (как в Rich FTAngle)
            Rotation cameraRotation = RotationUtil.fromVec2f(mc.player.getRotationClient());
            Rotation delta = RotationUtil.calculateDelta(currentRotation, cameraRotation);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

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
        } else {
            releasing = false;
        }

        if (attackedTicks > 0) attackedTicks = Math.max(attackedTicks - 1, 0);

        boolean intensiveAttack = System.currentTimeMillis() - lastAttack < 500;
        boolean lateAttack = System.currentTimeMillis() - lastAttack > 800;

        float prevPitch = targetRotation.getPitch();

        if (!MoveUtil.isMoving() && !attack && attackedTicks > 9 * 3) {
            float prevYaw = targetRotation.getYaw();

            float jitterPower = 45f + jitterUniquePower;
            float jitter = firstAttack ? 0f : idAttack == 0 ? -jitterPower : jitterPower;

            targetRotation = new Rotation(prevYaw + jitter, prevPitch);
        }

        float swing = (float) (Math.sin(mc.player.age * 0.9f) * MathUtil.randomInRange(16.8f, 17.1f));

        float yawSpeed = 69f / 3f;
        float pitchSpeed = 15f / 3f;

        if (lateAttack) {
            firstAttack = true;
            targetRotation = new Rotation(mc.player.getYaw(), prevPitch);
            yawSpeed *= 0.2f;
        }

        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        boolean glitchDelta = !attack && intensiveAttack && MoveUtil.isMoving();

        float finalDeltaYaw = glitchDelta ? swing / 3f : yawDelta + swing;
        float finalDeltaPitch = glitchDelta ? 0f : pitchDelta;

        float finalYaw = currentRotation.getYaw() + MathHelper.clamp(finalDeltaYaw, -yawSpeed, yawSpeed);
        float finalPitch = currentRotation.getPitch() + MathHelper.clamp(finalDeltaPitch, -pitchSpeed, pitchSpeed);

        return new Rotation(
                finalYaw, finalPitch
        );
    }
}

package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AuraModule;

import java.util.concurrent.ThreadLocalRandom;

public class GrimRotation extends RotationMode {
    private static float lastYaw = 0;
    private static float lastPitch = 0;
    private static long lastUpdateTime = 0;

    public GrimRotation() {
        super("Grim");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            // Плавное возвращение камеры как в MatrixRotation
            Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
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
        }

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        // Позиция с предсказанием
        Vec3d targetPos = predictTargetPosition(target, deltaTime);
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = targetPos.subtract(eyePos);

        // Базовые углы
        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float targetPitch = (float) Math.toDegrees(Math.asin(-direction.normalize().y));
        targetPitch = MathHelper.clamp(targetPitch, -89.0f, 89.0f);

        // В оригинале здесь была нейросеть, заменим её на небольшую рандомизацию и сглаживание
        // для стабильного обхода
        float jitterAmount = 0.5f;
        targetYaw += (ThreadLocalRandom.current().nextFloat() * 2 - 1) * jitterAmount;
        targetPitch += (ThreadLocalRandom.current().nextFloat() * 2 - 1) * jitterAmount;

        float currentYaw = currentRotation.getYaw();
        float currentPitch = currentRotation.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        // Адаптивная скорость
        float distance = mc.player.distanceTo(target);
        boolean canAttack = AuraModule.getInstance().getCombatExecutor().combatManager().canAttack();
        
        float speedMultiplier = calculateSpeedMultiplier(distance, canAttack);

        float yawSpeed = 45.0f * speedMultiplier;
        float pitchSpeed = 35.0f * speedMultiplier;

        if (!canAttack) {
            yawSpeed *= 0.7f;
            pitchSpeed *= 0.7f;
        }

        float smoothYaw = currentYaw + MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float smoothPitch = MathHelper.clamp(currentPitch + MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed), -89.0f, 89.0f);

        lastYaw = smoothYaw;
        lastPitch = smoothPitch;

        return new Rotation(smoothYaw, smoothPitch);
    }

    private Vec3d predictTargetPosition(LivingEntity target, float deltaTime) {
        Vec3d currentPos = new Vec3d(target.getX(), target.getY() + target.getEyeHeight(target.getPose()) * 0.9, target.getZ());
        Vec3d velocity = target.getVelocity();

        float predictionTime = MathHelper.clamp(deltaTime * 3.0f, 0.05f, 0.2f);
        return currentPos.add(velocity.multiply(predictionTime));
    }

    private float calculateSpeedMultiplier(float distance, boolean canAttack) {
        float distanceFactor = MathHelper.clamp(1.0f - (distance / 6.0f), 0.3f, 1.0f);
        float attackFactor = canAttack ? 1.3f : 1.0f;
        return distanceFactor * attackFactor;
    }
}

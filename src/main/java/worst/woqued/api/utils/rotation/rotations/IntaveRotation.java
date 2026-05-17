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

public class IntaveRotation extends RotationMode {
    private static float lastYaw = 0;
    private static float lastPitch = 0;
    private static float lastYawDelta = 0;
    private static float lastPitchDelta = 0;
    private static int tickCounter = 0;
    private static float accumulatedOffset = 0;

    public IntaveRotation() {
        super("Intave");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            return smoothReturn(currentRotation, targetRotation);
        }

        tickCounter++;

        Vec3d targetPos = getTargetPosition(target);
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = targetPos.subtract(eyePos);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float targetPitch = (float) Math.toDegrees(Math.asin(-direction.normalize().y));
        targetPitch = MathHelper.clamp(targetPitch, -89.0f, 89.0f);

        float currentYaw = currentRotation.getYaw();
        float currentPitch = currentRotation.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        float distance = mc.player.distanceTo(target);
        boolean canAttack = AuraModule.getInstance().getCombatExecutor().combatManager().canAttack();

        RotationSpeeds speeds = calculateRotationSpeeds(yawDelta, pitchDelta, distance, canAttack);

        yawDelta = applySoftGCD(yawDelta);
        pitchDelta = applySoftGCD(pitchDelta);

        yawDelta = addSubtleJitter(yawDelta, canAttack);
        pitchDelta = addSubtleJitter(pitchDelta, canAttack);

        yawDelta = avoidPerfectAngles(yawDelta);
        pitchDelta = avoidPerfectAngles(pitchDelta);

        yawDelta = clampRotationSpeed(yawDelta, speeds.yawSpeed);
        pitchDelta = clampRotationSpeed(pitchDelta, speeds.pitchSpeed);

        yawDelta = applySmoothTransition(yawDelta, lastYawDelta);
        pitchDelta = applySmoothTransition(pitchDelta, lastPitchDelta);

        float smoothYaw = currentYaw + yawDelta;
        float smoothPitch = MathHelper.clamp(currentPitch + pitchDelta, -89.0f, 89.0f);

        lastYaw = smoothYaw;
        lastPitch = smoothPitch;
        lastYawDelta = yawDelta;
        lastPitchDelta = pitchDelta;

        return new Rotation(smoothYaw, smoothPitch);
    }

    private Rotation smoothReturn(Rotation currentRotation, Rotation targetRotation) {
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

    private Vec3d getTargetPosition(LivingEntity target) {
        Vec3d velocity = target.getVelocity();
        float predictionTime = 0.05f;
        
        Vec3d predictedPos = new Vec3d(
                target.getX() + velocity.x * predictionTime,
                target.getY() + target.getEyeHeight(target.getPose()) * 0.9 + velocity.y * predictionTime,
                target.getZ() + velocity.z * predictionTime
        );
        
        return predictedPos;
    }

    private RotationSpeeds calculateRotationSpeeds(float yawDelta, float pitchDelta, float distance, boolean canAttack) {
        float distanceFactor = MathHelper.clamp(1.0f - (distance / 6.0f), 0.5f, 1.0f);
        float attackFactor = canAttack ? 1.3f : 1.0f;
        
        float baseYawSpeed = 40.0f * distanceFactor * attackFactor;
        float basePitchSpeed = 32.0f * distanceFactor * attackFactor;

        float rotationMagnitude = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        
        if (rotationMagnitude > 90) {
            baseYawSpeed *= 1.2f;
            basePitchSpeed *= 1.2f;
        }

        return new RotationSpeeds(baseYawSpeed, basePitchSpeed);
    }

    private float applySoftGCD(float delta) {
        if (Math.abs(delta) < 0.05f) {
            return delta;
        }

        float sensitivity = 0.15f;
        float gcd = sensitivity * 0.15f;
        
        if (gcd < 0.001f) {
            return delta;
        }

        float steps = Math.round(delta / gcd);
        float gcdResult = steps * gcd;
        
        float deviation = Math.abs(delta - gcdResult);
        if (deviation < 0.02f) {
            return gcdResult;
        }
        
        return delta;
    }

    private float addSubtleJitter(float delta, boolean canAttack) {
        if (Math.abs(delta) < 0.3f) {
            return delta;
        }

        float jitterAmount = canAttack ? 0.08f : 0.05f;
        
        if (tickCounter % 3 == 0) {
            float jitter = (ThreadLocalRandom.current().nextFloat() * 2 - 1) * jitterAmount;
            return delta + jitter;
        }
        
        return delta;
    }

    private float avoidPerfectAngles(float delta) {
        float absDelta = Math.abs(delta);
        
        if (absDelta < 0.05f) {
            accumulatedOffset += (ThreadLocalRandom.current().nextFloat() * 2 - 1) * 0.01f;
            accumulatedOffset = MathHelper.clamp(accumulatedOffset, -0.1f, 0.1f);
            return delta + accumulatedOffset;
        }

        if (absDelta > 0.1f && absDelta < 0.3f) {
            float offset = (ThreadLocalRandom.current().nextFloat() * 2 - 1) * 0.02f;
            return delta + offset;
        }

        return delta;
    }

    private float clampRotationSpeed(float delta, float maxSpeed) {
        float absDelta = Math.abs(delta);
        
        if (absDelta > maxSpeed) {
            float sign = Math.signum(delta);
            delta = sign * maxSpeed;
        }

        return delta;
    }

    private float applySmoothTransition(float delta, float lastDelta) {
        if (Math.abs(delta) < 0.1f) {
            return delta;
        }

        float smoothingFactor = 0.7f;
        float smoothedDelta = lastDelta * smoothingFactor + delta * (1 - smoothingFactor);
        
        return smoothedDelta;
    }

    private static class RotationSpeeds {
        final float yawSpeed;
        final float pitchSpeed;

        RotationSpeeds(float yawSpeed, float pitchSpeed) {
            this.yawSpeed = yawSpeed;
            this.pitchSpeed = pitchSpeed;
        }
    }
}
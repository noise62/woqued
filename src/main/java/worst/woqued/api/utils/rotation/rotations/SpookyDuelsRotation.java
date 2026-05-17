package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;

import java.security.SecureRandom;

public class SpookyDuelsRotation extends RotationMode {
    private final SecureRandom random = new SecureRandom();
    private long lastPointChange = 0;
    private Vec3d currentTarget = null;
    private int targetBodyPart = 0;
    private long lastBodyPartChange = 0;

    public SpookyDuelsRotation() {
        super("Spooky Duels");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (entity == null) {
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

        if (currentTarget == null || currentTime - lastPointChange > 50 + random.nextInt(100)) {
            currentTarget = getMultiPoint(entity);
            lastPointChange = currentTime;
        }

        Rotation calculatedRotation = RotationUtil.fromVec3d(currentTarget.subtract(mc.player.getEyePos()));
        float targetYaw = calculatedRotation.getYaw();
        float targetPitch = calculatedRotation.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentRotation.getYaw());
        float pitchDelta = targetPitch - currentRotation.getPitch();

        if (Math.abs(yawDelta) == 0 && Math.abs(pitchDelta) > 0) {
            yawDelta += random(0.1f, 0.5f) + 0.1f * 1.0313f;
        }
        if (Math.abs(pitchDelta) == 0 && Math.abs(yawDelta) > 0) {
            pitchDelta += random(0.1f, 0.5f) + 0.1f * 1.0313f;
        }

        yawDelta = Math.min(Math.abs(yawDelta), 60 + (random.nextFloat() * 1.0329834f)) * Math.signum(yawDelta);
        pitchDelta = Math.min(Math.abs(pitchDelta), random(23.133f, 26.477f)) * Math.signum(pitchDelta);

        float distance = (float) mc.player.distanceTo(entity);
        float speed = calculateDynamicSpeed(distance, yawDelta, pitchDelta);

        float breathX = (float) Math.sin(currentTime / 300.0) * 0.03f;
        float breathY = (float) Math.cos(currentTime / 500.0) * 0.02f;

        float jitterYaw = 0;
        float jitterPitch = 0;
        if (random.nextFloat() < 0.02f) {
            jitterYaw = (random.nextFloat() - 0.5f) * 1.2f;
            jitterPitch = (random.nextFloat() - 0.5f) * 0.8f;
        }

        float smoothYaw = yawDelta * speed * 0.7f + breathX + jitterYaw;
        float smoothPitch = pitchDelta * speed * 0.5f + breathY + jitterPitch;

        if (random.nextFloat() < 0.05f && Math.abs(yawDelta) > 5.0f) {
            float overshoot = 1.1f + random.nextFloat() * 0.3f;
            smoothYaw *= overshoot;
        }

        float newYaw = currentRotation.getYaw() + smoothYaw;
        float newPitch = currentRotation.getPitch() + smoothPitch;

        newPitch = MathHelper.clamp(newPitch, -90.0f, 90.0f);

        return new Rotation(newYaw, newPitch);
    }

    private Vec3d getMultiPoint(Entity entity) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastBodyPartChange > 800 + random.nextInt(400)) {
            targetBodyPart = random.nextInt(3);
            lastBodyPartChange = currentTime;
        }

        double width = entity.getWidth();
        double height = entity.getHeight();

        float yawRange = random(15f, 36f);
        float pitchRange = random(4f, 24f);

        double randomX = entity.getX() + (random.nextGaussian() * 0.4) * width;
        double randomZ = entity.getZ() + (random.nextGaussian() * 0.4) * width;

        double randomY;
        switch (targetBodyPart) {
            case 0:
                randomY = entity.getY() + height * (double) random(0.6f, 0.8f);
                break;
            case 1:
                randomY = entity.getY() + height * (double) random(0.85f, 0.95f);
                break;
            default:
                randomY = entity.getY() + height * (double) random(0.4f, 0.6f);
                break;
        }

        randomX += Math.sin(currentTime / 200.0) * width * 0.1;
        randomZ += Math.cos(currentTime / 200.0) * width * 0.1;

        return new Vec3d(randomX, randomY, randomZ);
    }

    private float calculateDynamicSpeed(float distance, float yawDelta, float pitchDelta) {
        float neuroRand = random.nextFloat();

        float distanceFactor;
        if (distance < 2.0f) {
            distanceFactor = 1.4f + neuroRand * 0.6f;
        } else if (distance < 4.0f) {
            distanceFactor = 1.2f + neuroRand * 0.6f;
        } else {
            distanceFactor = 0.9f + neuroRand * 0.5f;
        }

        float smoothFactorBase;
        if (distance < 3.0f && Math.abs(yawDelta) < 10.0f) {
            smoothFactorBase = 0.25f + random.nextFloat() * 0.15f;
        } else if (distance > 5.0f || Math.abs(yawDelta) > 30.0f) {
            smoothFactorBase = 0.35f + random.nextFloat() * 0.2f;
        } else {
            smoothFactorBase = 0.28f + random.nextFloat() * 0.15f;
        }

        return smoothFactorBase * distanceFactor;
    }

    private float random(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
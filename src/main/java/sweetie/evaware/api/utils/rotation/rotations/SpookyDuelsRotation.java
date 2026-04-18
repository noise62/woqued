package sweetie.evaware.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;

import java.util.Random;

public class SpookyDuelsRotation extends RotationMode {
    private final Random random = new Random();
    private float lastYaw = 0;
    private float lastPitch = 0;
    private Entity lastEntity = null;
    private float velocityYaw = 0;
    private float velocityPitch = 0;

    private double curDiffX = 0;
    private double curDiffY = 0;
    private double curDiffZ = 0;
    private int targetTick = 0;

    public SpookyDuelsRotation() {
        super("Spooky Duels");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (entity == null) {
            lastEntity = null;
            velocityYaw = 0;
            velocityPitch = 0;

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

        if (entity != lastEntity) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
            velocityYaw = 0;
            velocityPitch = 0;
            lastEntity = entity;
        }

        Vec3d aimPoint = getAimPoint(entity);
        Rotation calculated = RotationUtil.rotationAt(aimPoint);

        float targetYaw = calculated.getYaw();
        float targetPitch = MathHelper.clamp(calculated.getPitch(), -90f, 90f);

        float yawDiff = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float pitchDiff = targetPitch - lastPitch;

        float softness = 0.55f + (random.nextFloat() * 0.25f);
        float accelYaw = yawDiff * softness;
        float accelPitch = pitchDiff * softness;

        float friction = 0.35f + (random.nextFloat() * 0.2f);
        velocityYaw = (velocityYaw * friction + accelYaw);
        velocityPitch = (velocityPitch * friction + accelPitch);

        float maxStep = 85.0f;
        velocityYaw = MathHelper.clamp(velocityYaw, -maxStep, maxStep);
        velocityPitch = MathHelper.clamp(velocityPitch, -maxStep, maxStep);

        float newYaw = lastYaw + velocityYaw;
        float newPitch = MathHelper.clamp(lastPitch + velocityPitch, -90f, 90f);

        if (Math.abs(yawDiff) < 0.5f) {
            newYaw = lastYaw + (yawDiff * 0.5f);
        }

        lastYaw = newYaw;
        lastPitch = newPitch;

        return new Rotation(newYaw, newPitch).adjustSensitivity();
    }

    private Vec3d getAimPoint(Entity entity) {
        var box = entity.getBoundingBox();

        targetTick--;
        if (targetTick <= 0) {
            targetTick = 4 + random.nextInt(8);
            double rangeX = (box.maxX - box.minX) * 0.3;
            double rangeZ = (box.maxZ - box.minZ) * 0.3;
            double rangeY = (box.maxY - box.minY) * 0.2;

            curDiffX = (random.nextDouble() - 0.5) * rangeX;
            curDiffZ = (random.nextDouble() - 0.5) * rangeZ;
            curDiffY = (random.nextDouble() - 0.5) * rangeY;
        }

        double centerX = (box.minX + box.maxX) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;
        double centerY = box.minY + ((box.maxY - box.minY) * 0.55);

        return new Vec3d(centerX + curDiffX, centerY + curDiffY, centerZ + curDiffZ);
    }
}

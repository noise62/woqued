package worst.woqued.client.features.modules.movement.speed.modes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;

import java.util.Random;
import java.util.function.Supplier;

public class SpeedAresEntity extends SpeedMode {
    private final double baseSpeed = 0.085;
    private final double maxSpeed = 0.38;
    private final double expandBox = 0.45;
    private final double skipChance = 0.25;
    private final double sprintAngle = 80.0;
    private final double walkAngle = 30.0;
    private final Random random = new Random();

    public SpeedAresEntity(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "AresEntity";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;
        if (!MoveUtil.isMoving()) return;

        boolean isSprinting = mc.player.isSprinting();
        double currentSkipChance = isSprinting ? 0.8 : 0.3;

        if (random.nextDouble() < currentSkipChance) {
            return;
        }

        Box checkBox = mc.player.getBoundingBox().expand(expandBox);
        Entity nearest = null;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof PlayerEntity)) continue;
            if (!(entity instanceof LivingEntity) && !(entity instanceof BoatEntity)) continue;

            if (checkBox.intersects(entity.getBoundingBox())) {
                nearest = entity;
                break;
            }
        }

        if (nearest == null) return;

        double speedMultiplier = 0.05;
        speedMultiplier *= (0.8 + random.nextDouble() * 0.4);

        Vec3d directionToTarget = nearest.getPos().subtract(mc.player.getPos()).normalize();

        double angleRange = isSprinting ? sprintAngle : walkAngle;
        double randomAngle = (random.nextDouble() - 0.5) * Math.toRadians(angleRange);

        double sin = Math.sin(randomAngle);
        double cos = Math.cos(randomAngle);

        double nx = directionToTarget.x * cos - directionToTarget.z * sin;
        double nz = directionToTarget.x * sin + directionToTarget.z * cos;

        Vec3d finalDir = new Vec3d(nx, 0, nz).normalize();

        Vec3d currentVel = mc.player.getVelocity();
        Vec3d addVel = finalDir.multiply(speedMultiplier);
        Vec3d newVel = currentVel.add(addVel.x, 0, addVel.z);

        double hSpeed = Math.sqrt(newVel.x * newVel.x + newVel.z * newVel.z);
        double speedLimit = isSprinting ? 0.35 : 0.4;

        if (hSpeed > speedLimit) {
            double scale = speedLimit / hSpeed;
            newVel = new Vec3d(newVel.x * scale, newVel.y, newVel.z * scale);
        }

        mc.player.setVelocity(newVel);
    }
}

package sweetie.evaware.api.utils.rotation;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.rotation.manager.Rotation;

@UtilityClass
public class RotationUtil implements QuickImports {
    public boolean inFov(Vec3d vec3d, float fov) {
        Rotation rotation = rotationAt(vec3d);
        float deltaYaw = MathHelper.wrapDegrees(rotation.getYaw() - mc.player.getYaw());
        float deltaPitch = MathHelper.wrapDegrees(rotation.getPitch() - mc.player.getPitch());

        return Math.abs(deltaYaw) <= fov && Math.abs(deltaPitch) <= fov;
    }

    public Rotation rotationAt(Vec3d position) {
        if (position == null) return Rotation.DEFAULT;
        Vec3d playerPos = mc.player.getPos().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
        float diffX = (float) (position.x - playerPos.x);
        float diffY = (float) (position.y - playerPos.y);
        float diffZ = (float) (position.z - playerPos.z);

        float dist = (float) Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new Rotation(yaw, pitch);
    }

    public Rotation fromVec2f(Vec2f vector2f) {
        return new Rotation(vector2f.y, vector2f.x);
    }

    public Rotation fromVec3d(Vec3d vector) {
        return new Rotation(
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90),
                MathHelper.wrapDegrees((float) Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
        );
    }

    public Rotation calculateDelta(Rotation start, Rotation end) {
        float deltaYaw = MathHelper.wrapDegrees(end.getYaw() - start.getYaw());
        float deltaPitch = MathHelper.wrapDegrees(end.getPitch() - start.getPitch());
        return new Rotation(deltaYaw, deltaPitch);
    }

    public Vec3d getSpot(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        return new Vec3d(
                MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ)
        );
    }

    public Vec3d rayCastBox(Entity entity, Vec3d end) {
        Box box = entity.getBoundingBox();
        Vec3d start = mc.getCameraEntity().getCameraPosVec(1.0f);

        Vec3d min = new Vec3d(box.minX, box.minY, box.minZ);
        Vec3d max = new Vec3d(box.maxX, box.maxY, box.maxZ);

        double tMin = -Double.MAX_VALUE;
        double tMax = Double.MAX_VALUE;
        Vec3d direction = end.normalize();

        for (int axis = 0; axis < 3; axis++) {
            double d, minVal, maxVal, startVal;

            switch (axis) {
                case 0: d = direction.x; minVal = min.x; maxVal = max.x; startVal = start.x; break;
                case 1: d = direction.y; minVal = min.y; maxVal = max.y; startVal = start.y; break;
                case 2: d = direction.z; minVal = min.z; maxVal = max.z; startVal = start.z; break;
                default: continue;
            }

            if (Math.abs(d) < 1e-7) {
                if (startVal < minVal || startVal > maxVal) {
                    return end;
                }
            } else {
                double t1 = (minVal - startVal) / d;
                double t2 = (maxVal - startVal) / d;

                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);

                if (tMin > tMax) {
                    return end;
                }
            }
        }

        double distance = start.distanceTo(end);
        if (tMin > distance || tMin < 0) {
            return end;
        }

        return start.add(direction.multiply(tMin));
    }
}

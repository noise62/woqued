package worst.woqued.client.features.modules.movement.speed.modes;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;

import java.util.function.Supplier;

public class SpeedAresEntity extends SpeedMode {
    private static final double BASE_SPEED = 0.0006;
    private static final double MAX_RANGE = 0.2;

    public SpeedAresEntity(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "AresEntity";
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        Entity nearest = null;
        double bestSq = Double.MAX_VALUE;
        double maxRangeSq = MAX_RANGE * MAX_RANGE;

        for (Entity ent : mc.world.getEntities()) {
            if (ent == mc.player) continue;

            double dx = ent.getX() - mc.player.getX();
            double dz = ent.getZ() - mc.player.getZ();
            double sq = dx * dx + dz * dz;

            if (sq <= maxRangeSq && sq < bestSq) {
                bestSq = sq;
                nearest = ent;
            }
        }

        if (nearest != null) {
            double[] dir = getDirectionToPoint(
                    new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                    new Vec3d(nearest.getX(), nearest.getY(), nearest.getZ()),
                    BASE_SPEED
            );
            mc.player.addVelocity(dir[0], 0.0, dir[1]);
        }
    }

    private double[] getDirectionToPoint(Vec3d from, Vec3d to, double spd) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        return len == 0.0 ? new double[]{0.0, 0.0} : new double[]{dx / len * spd, dz / len * spd};
    }
}

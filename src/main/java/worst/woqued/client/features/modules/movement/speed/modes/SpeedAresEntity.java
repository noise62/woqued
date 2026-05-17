package worst.woqued.client.features.modules.movement.speed.modes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;

import java.util.function.Supplier;

public class SpeedAresEntity extends SpeedMode {
    public SpeedAresEntity(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "AresEntity";
    }

    @Override
    public void onTravel() {
        if (mc.player == null || mc.world == null) return;
        if (!MoveUtil.isMoving()) return;

        int collisions = 0;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living) {
                if (living == mc.player) continue;
                if (living instanceof ArmorStandEntity) continue;
                if (PlayerUtil.hasCollisionWith(living, 0f)) {
                    collisions++;
                }
            }
        }

        if (collisions > 0) {
            double[] forward = MoveUtil.forward(0.08 * collisions);
            mc.player.addVelocity(forward[0], 0.0, forward[1]);
        }
    }
}
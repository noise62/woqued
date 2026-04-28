package worst.woqued.client.features.modules.movement.speed.modes;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.client.features.modules.movement.speed.SpeedMode;

import java.util.function.Supplier;

public class SpeedMetaHvH extends SpeedMode {
    private static final float META_HVH_BASE_SPEED = 0.36f;

    public SpeedMetaHvH(Supplier<Boolean> condition) {
    }

    @Override
    public String getName() {
        return "MetaHvH";
    }

    @Override
    public void onTravel() {
        if (mc.player == null) return;

        ItemStack offhandItem = mc.player.getOffHandStack();
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        String itemName = offhandItem.isEmpty() ? "" : offhandItem.getName().getString();

        float appliedSpeed;
        if (speedEffect != null) {
            int amplifier = speedEffect.getAmplifier();
            if (amplifier >= 2) {
                appliedSpeed = META_HVH_BASE_SPEED * 1.155f;
            } else if (amplifier >= 1) {
                appliedSpeed = META_HVH_BASE_SPEED;
            } else {
                appliedSpeed = META_HVH_BASE_SPEED * 0.85f;
            }
        } else {
            appliedSpeed = META_HVH_BASE_SPEED * 0.68f;
        }

        if (!itemName.isEmpty() && itemName.contains("Ломтик Дыни")) {
            appliedSpeed = speedEffect != null && speedEffect.getAmplifier() >= 2 ? 0.41755f : 0.41755f * 0.52f;
        }

        if (slowEffect != null) {
            appliedSpeed *= 0.835f;
        }

        if (!mc.player.isOnGround()) {
            appliedSpeed *= 1.435f;
        }

        MoveUtil.setSpeed(appliedSpeed);
    }
}

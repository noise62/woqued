package sweetie.evaware.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;

@UtilityClass
public class AuraUtil implements QuickImports {
    private float hitCount = 0;
    private TimerUtil attackTimer = new TimerUtil();

    public void onAttack(String mode) {
        switch (mode) {
            case "Grim" -> {
                // Grim doesn't need special attack handling
            }

            case "Sloth" -> {
                // Sloth handles attack in AuraModule directly
            }

            default -> {
                hitCount += 1;
                if (hitCount >= 3) {
                    hitCount = 0;
                }
            }
        }
    }
    public Vec3d getAimpoint(LivingEntity entity, String mode) {
        switch (mode) {
            case "Grim" -> {
                return RotationUtil.getSpot(entity);
            }

            case "Sloth" -> {
                // SlothRotation calculates its own aim points
                return RotationUtil.getSpot(entity);
            }

            default -> {
                return RotationUtil.getSpot(entity);
            }
        }
    }

    public Vec3d getBestVector(Entity entity, float jitterOnBoxValue) {
        double yExpand = MathHelper.clamp(mc.player.getEyeHeight(mc.player.getPose()) - entity.getEyeHeight(entity.getPose()), entity.getHeight() / 2, entity.getHeight())
                / (mc.player.isGliding() ? 10 : !mc.options.jumpKey.isPressed() && mc.player.isOnGround() ?
                entity.isSneaking() ? 0.8F : 0.6f : 1F);

        Vec3d finalVector = entity.getPos().add(0, yExpand, 0);
        return finalVector.add(jitterOnBoxValue, jitterOnBoxValue / 2, jitterOnBoxValue);
    }
}
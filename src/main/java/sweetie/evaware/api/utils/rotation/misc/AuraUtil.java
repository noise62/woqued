package sweetie.evaware.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.rotations.FTSnapRotation;
import sweetie.evaware.client.features.modules.combat.AuraModule;

@UtilityClass
public class AuraUtil implements QuickImports {
    private float hitCount = 0;
    private TimerUtil attackTimer = new TimerUtil();

    public void onAttack(String mode) {
        switch (mode) {
            case "Ft snap" -> {
                FTSnapRotation.updateAttackState(true);
            }

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
            case "Ft snap" -> {
                return getDistanceBasedPoint(entity);
            }

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

    public Vec3d getDistanceBasedPoint(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();

        float attackDistance = AuraModule.getInstance().getAttackDistance() + AuraModule.getInstance().getPreDistance();
        float distanceFactor = (float) (mc.player.getPos().distanceTo(entity.getPos()) / attackDistance);

        float minY = (float) (box.maxY - box.minY);
        float clampedY = (float) Math.max(box.minY + minY * distanceFactor, box.minY + minY * 0.3f);

        float safePoint = entity.getWidth() * 0.4f;

        Vec3d basePoint = new Vec3d(
                MathHelper.clamp(eye.x, box.minX + safePoint, box.maxX - safePoint),
                MathHelper.clamp(eye.y, box.minY, clampedY),
                MathHelper.clamp(eye.z, box.minZ + safePoint, box.maxZ - safePoint)
        );

        return basePoint;
    }
}
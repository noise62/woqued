package sweetie.evaware.inject.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sweetie.evaware.api.system.backend.Pair;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;
import sweetie.evaware.client.features.modules.movement.nitrofirework.NitroFireworkModule;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {
    @Shadow
    private LivingEntity shooter;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d fixFireworkVelocity(LivingEntity instance) {
        if (instance != SharedClass.player()) {
            return instance.getRotationVector();
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return instance.getRotationVector();
        }

        return rotation.getVector();
    }


    @ModifyArgs(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;", ordinal = 0))
    private void hookExtendedFirework(Args args, @Local(ordinal = 0) Vec3d rotation, @Local(ordinal = 1) Vec3d velocity) {
        if (shooter != SharedClass.player() || !NitroFireworkModule.getInstance().isEnabled()) return;

        Pair<Float, Float> pair = NitroFireworkModule.getInstance().currentMode.velocityValues();

        float donichka = 0.1f;
        float piska = 0.5f;
        Vec2f multiplier = new Vec2f(pair.left(), pair.right());
        args.set(0, rotation.x * donichka + (rotation.x * multiplier.x - velocity.x) * piska);
        args.set(1, rotation.y * donichka + (rotation.y * multiplier.y - velocity.y) * piska);
        args.set(2, rotation.z * donichka + (rotation.z * multiplier.x - velocity.z) * piska);
    }
}

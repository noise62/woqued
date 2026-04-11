package sweetie.evaware.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweetie.evaware.api.event.events.player.move.JumpEvent;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;
import sweetie.evaware.client.features.modules.combat.VelocityModule;
import sweetie.evaware.client.features.modules.render.SwingAnimationModule;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @Shadow
    public abstract boolean isGliding();

    @Shadow
    public int jumpingCooldown;

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getArmSwingAnimationEnd(final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        SwingAnimationModule swingAnim = SwingAnimationModule.getInstance();
        if (swingAnim.slow.getValue() && swingAnim.isEnabled())
            callbackInfoReturnable.setReturnValue(swingAnim.speed.getValue().intValue());
    }

    @Inject(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getJumpVelocity()F"))
    public void onJumping(CallbackInfo ci) {
        if (((Object) this) != SharedClass.player()) {
            return;
        }

        JumpEvent.getInstance().call();
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void noPushHookByEntity(CallbackInfo ci) {
        if (VelocityModule.getInstance().cancelPush(VelocityModule.PushingSource.ENTITY)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    public float fixJumpVelocity(float original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null || !currentRotationPlan.moveCorrection()) {
            return original;
        }

        return rotation.getYaw();
    }

    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float fixGlidingVelocity(float original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null || !currentRotationPlan.moveCorrection()) {
            return original;
        }

        return rotation.getPitch();
    }


    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d fixGlidingVelocityVector(Vec3d original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null || !currentRotationPlan.moveCorrection()) {
            return original;
        }

        return rotation.getVector();
    }
}

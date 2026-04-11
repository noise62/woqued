package sweetie.evaware.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweetie.evaware.api.event.events.player.move.VelocityEvent;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.client.features.modules.combat.VelocityModule;
import sweetie.evaware.client.features.modules.render.RemovalsModule;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @ModifyExpressionValue(method = "updateMovementInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;getVelocity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d noPushInLiquidsHook(Vec3d original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        return !VelocityModule.getInstance().cancelPush(VelocityModule.PushingSource.LIQUIDS) ? original : Vec3d.ZERO;
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public void cancelGlowingHook(CallbackInfoReturnable<Boolean> cir) {
        if (RemovalsModule.getInstance().isGlowEffect()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean fixFallDistance(boolean original) {
        if ((Object) this == SharedClass.player()) {
            return false;
        }

        return original;
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d updateVelocityHook(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == SharedClass.player()) {
            VelocityEvent.VelocityEventData event = new VelocityEvent.VelocityEventData(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
            VelocityEvent.getInstance().call(event);
            return event.getVelocity();
        }


        return Entity.movementInputToVelocity(movementInput, speed, yaw);
    }
}

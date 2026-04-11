package sweetie.evaware.inject.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.player.world.AttackEvent;
import sweetie.evaware.api.event.events.player.move.TravelEvent;
import sweetie.evaware.api.system.backend.SharedClass;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {
    @Inject(method = "attack", at = @At("HEAD"))
    public void attackEventHook(Entity target, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        if ((Object) this == SharedClass.player()) {
            AttackEvent.getInstance().call(new AttackEvent.AttackEventData(target));
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    public void travelHook(Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this != SharedClass.player()) {
            return;
        }

        TravelEvent.getInstance().call();
    }
}

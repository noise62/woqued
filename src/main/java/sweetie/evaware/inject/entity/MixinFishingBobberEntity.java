package sweetie.evaware.inject.entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.client.features.modules.combat.VelocityModule;

@Mixin(FishingBobberEntity.class)
public abstract class MixinFishingBobberEntity {
    @WrapWithCondition(method = "handleStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;pullHookedEntity(Lnet/minecraft/entity/Entity;)V"))
    private boolean noPushByFishingRodHook(FishingBobberEntity instance, Entity entity) {
        return entity != SharedClass.player() || VelocityModule.getInstance().cancelPush(VelocityModule.PushingSource.FISHING_ROD);
    }

}


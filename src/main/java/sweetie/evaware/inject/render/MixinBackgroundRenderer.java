package sweetie.evaware.inject.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweetie.evaware.client.features.modules.render.AmbienceModule;
import sweetie.evaware.client.features.modules.render.RemovalsModule;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
        if (RemovalsModule.getInstance().isBadEffects()) {
            info.setReturnValue(null);
        }
    }

    @ModifyReturnValue(method = "applyFog", at = @At("RETURN"))
    private static Fog onApplyFog(Fog original, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true, ordinal = 0) float viewDistance) {
        return AmbienceModule.getInstance().applyCustomFog(camera, viewDistance, original);
    }
}

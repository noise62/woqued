package sweetie.evaware.inject.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.client.features.modules.render.AmbienceModule;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Inject(method = "clearColor", at = @At(value = "HEAD"), cancellable = true)
    private static void fogColor(float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (AmbienceModule.getInstance().applyBackgroundColor()) {
            ci.cancel();
        }
    }
}

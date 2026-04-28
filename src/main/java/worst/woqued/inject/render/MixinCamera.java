package worst.woqued.inject.render;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import worst.woqued.client.features.modules.render.SmoothCameraModule;

@Mixin(Camera.class)
public class MixinCamera {

    @Shadow private Vec3d pos;

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(CallbackInfo ci) {
        SmoothCameraModule module = SmoothCameraModule.getInstance();
        if (!module.isEnabled()) return;

        module.updateCamera(pos);

        pos = module.getSmoothPos();
    }
}
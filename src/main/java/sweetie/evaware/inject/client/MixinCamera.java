package sweetie.evaware.inject.client;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sweetie.evaware.client.features.modules.render.CameraClipModule;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    protected abstract float clipToSpace(float desiredCameraDistance);

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    private void update(Args args) {
        if (CameraClipModule.getInstance().isEnabled()) {
            args.set(0, -clipToSpace(CameraClipModule.getInstance().distance.getValue()));
        }
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void clipToSpace(float f, CallbackInfoReturnable<Float> returnable) {
        if (CameraClipModule.getInstance().isEnabled()) {
            returnable.setReturnValue(CameraClipModule.getInstance().distance.getValue());
        }
    }
}

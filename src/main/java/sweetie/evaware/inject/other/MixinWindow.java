package sweetie.evaware.inject.other;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.other.FramebufferResizeEvent;
import sweetie.evaware.api.event.events.other.WindowResizeEvent;

@Mixin(Window.class)
public class MixinWindow {
    @Shadow @Final private long handle;

    @Inject(method = "onWindowSizeChanged", at = @At("RETURN"))
    public void windowResizeHook(long window, int width, int height, CallbackInfo ci) {
        WindowResizeEvent.getInstance().call();
    }

    @Inject(method = "onFramebufferSizeChanged", at = @At("RETURN"))
    public void framebufferResizeHook(long window, int width, int height, CallbackInfo callbackInfo) {
        if (window == handle) {
            FramebufferResizeEvent.getInstance().call();
        }
    }
}

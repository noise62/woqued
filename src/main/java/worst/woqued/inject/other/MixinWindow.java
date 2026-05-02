package worst.woqued.inject.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import worst.woqued.api.event.events.other.FramebufferResizeEvent;
import worst.woqued.api.event.events.other.WindowResizeEvent;

@Mixin(Window.class)
public class MixinWindow {
    @Shadow @Final private long handle;
    private static final String CUSTOM_TITLE = "Woqued | 1.7.0";

    @Inject(method = "onWindowSizeChanged", at = @At("RETURN"))
    public void windowResizeHook(long window, int width, int height, CallbackInfo ci) {
        WindowResizeEvent.getInstance().call();
        if (window == handle) {
            updateTitle();
        }
    }

    @Inject(method = "onFramebufferSizeChanged", at = @At("RETURN"))
    public void framebufferResizeHook(long window, int width, int height, CallbackInfo callbackInfo) {
        if (window == handle) {
            FramebufferResizeEvent.getInstance().call();
        }
    }

    @Inject(method = "onWindowFocusChanged", at = @At("RETURN"))
    public void onWindowFocusChanged(long window, boolean focused, CallbackInfo ci) {
        if (window == handle && focused) {
            updateTitle();
        }
    }

    private void updateTitle() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getSession() != null) {
            String username = mc.getSession().getUsername();
            if (username != null) {
                mc.getWindow().setTitle(CUSTOM_TITLE);
            }
        }
    }
}
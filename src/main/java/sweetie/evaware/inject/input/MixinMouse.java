package sweetie.evaware.inject.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.client.KeyEvent;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.system.draggable.DraggableManager;
import sweetie.evaware.client.ui.clickgui.ScreenClickGUI;

@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(method = "lockCursor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"), cancellable = true)
    private void lockCursorHook(CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ScreenClickGUI) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/InactivityFpsLimiter;onInput()V"))
    public void mousePressHook(long window, int button, int action, int mods, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        KeyEvent.getInstance().call(new KeyEvent.KeyEventData(button, action, true));

        DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
            if (draggable.getModule().isEnabled()) {
                if (action == 0) {
                    draggable.onRelease(button);
                } else if (action == 1) {
                    draggable.onClick(button);
                }
            }
        });
    }
}

package sweetie.evaware.inject.input;

import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.client.KeyEvent;
import sweetie.evaware.api.system.backend.SharedClass;

@Mixin(Keyboard.class)
public class MixinKeyboard {
    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/InactivityFpsLimiter;onInput()V"))
    public void keyPressHook(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        KeyEvent.getInstance().call(new KeyEvent.KeyEventData(key, action, false));
    }
}

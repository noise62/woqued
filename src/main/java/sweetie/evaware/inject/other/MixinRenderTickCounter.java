package sweetie.evaware.inject.other;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweetie.evaware.api.system.client.TimerManager;

@Mixin(RenderTickCounter.Dynamic.class)
public class MixinRenderTickCounter {
    @Shadow
    private float lastFrameDuration;

    @Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastFrameDuration:F", shift = At.Shift.AFTER))
    private void timer(CallbackInfoReturnable<Integer> callback) {
        float customTimer = TimerManager.getInstance().getTimerSpeed();
        if (customTimer > 0) {
            lastFrameDuration *= customTimer;
        }
    }
}
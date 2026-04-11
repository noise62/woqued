package sweetie.evaware.inject.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.EvaWare;
import sweetie.evaware.api.event.events.client.GameLoopEvent;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.framelimiter.FrameLimiter;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Unique
    private final FrameLimiter frameLimiter = new FrameLimiter(false);

    @Inject(method = "render", at = @At("HEAD"))
    public void gameLoopHook(boolean tick, CallbackInfo ci) {
        frameLimiter.execute(60, () -> GameLoopEvent.getInstance().call());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void preTickHook(CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        TickEvent.getInstance().call();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void closeHook(CallbackInfo ci) {
        EvaWare.getInstance().onClose();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(RunArgs args, CallbackInfo ci) {
        EvaWare.getInstance().postLoad();
    }
}

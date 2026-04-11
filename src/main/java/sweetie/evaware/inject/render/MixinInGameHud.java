package sweetie.evaware.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.utils.render.KawaseBlurProgram;
import sweetie.evaware.client.features.modules.render.InterfaceModule;
import sweetie.evaware.client.features.modules.render.RemovalsModule;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Inject(method = "render", at = @At("HEAD"))
    public void renderHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        KawaseBlurProgram.render(context.getMatrices());

        Render2DEvent.getInstance().call(new Render2DEvent.Render2DEventData(context, context.getMatrices(), tickCounter.getTickDelta(false)));
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Potions")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        if (RemovalsModule.getInstance().isScoreboard()) {
            ci.cancel();
        }
    }
}

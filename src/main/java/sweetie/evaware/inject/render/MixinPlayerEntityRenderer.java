package sweetie.evaware.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.client.features.modules.other.HealthResolverModule;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
    public MixinPlayerEntityRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("HEAD"))
    private void rwHealthFix(AbstractClientPlayerEntity abstractClientPlayerEntity, PlayerEntityRenderState playerEntityRenderState, float f, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null || client.world == null) return;

        if (!HealthResolverModule.getInstance().isRW()) return;


        Scoreboard scoreboard = abstractClientPlayerEntity.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

        if (scoreboardObjective != null) {
            for (PlayerEntity player : client.world.getPlayers()) {
                if (player == null || player.equals(client.player)) continue;

                try {
                    ScoreAccess score = scoreboard.getOrCreateScore(player, scoreboardObjective);
                    String scoreText = (score == null ? 20 : score.getScore()) + " " + scoreboardObjective.getDisplayName().getString();
                    String scoreNumber = scoreText.replaceAll("[^0-9]", "");
                    int hp = Integer.parseInt(scoreNumber);

                    if (hp >= 0 && hp <= player.getMaxHealth()) {
                        player.setHealth((float) hp);
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}

package sweetie.evaware.client.features.modules.render.nametags;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NameTagsPotions {
    private final NameTagsModule module;

    public NameTagsPotions(NameTagsModule module) {
        this.module = module;
    }

    public void renderPotions(PlayerEntity player, float x, float y, DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();

        if (player.getStatusEffects().isEmpty()) {
            return;
        }

        float scale = module.scale.getValue();
        float gap = 2f * scale;
        float fontSize = 7f * scale;

        for (StatusEffectInstance effect : player.getStatusEffects()) {
            String effectText = getEffectName(effect) + TextUtil.getDurationText(effect.getDuration());

            float effectNameWidth = Fonts.PS_MEDIUM.getWidth(effectText, fontSize);

            Fonts.PS_MEDIUM.drawText(matrixStack, effectText, x + gap, y, fontSize, module.textColor.getValue());

            Fonts.PS_MEDIUM.drawText(matrixStack, effectText, x + gap + effectNameWidth, y, fontSize, new Color(200, 200, 200));

            y += fontSize + gap;
        }
    }

    private String getEffectName(StatusEffectInstance effect) {
        String translationKey = effect.getTranslationKey();
        String name = translationKey.substring(translationKey.lastIndexOf(".") + 1);
        name = name.substring(0, 1).toUpperCase() + name.substring(1);

        if (effect.getAmplifier() > 0) {
            name += " " + (effect.getAmplifier() + 1);
        }

        return name.replaceAll("_", " ");
    }
}

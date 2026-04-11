package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;

public class PotionsWidget extends ContainerWidget {
    private final Map<String, Float> animMap = new HashMap<>();

    public PotionsWidget() {
        super(3f, 120f);
    }

    @Override
    public String getName() {
        return "Potions";
    }

    private static final Identifier[] BAD_EFFECTS = {
            Identifier.of("minecraft", "wither"),
            Identifier.of("minecraft", "poison"),
            Identifier.of("minecraft", "slowness"),
            Identifier.of("minecraft", "weakness"),
            Identifier.of("minecraft", "mining_fatigue"),
            Identifier.of("minecraft", "nausea"),
            Identifier.of("minecraft", "blindness"),
            Identifier.of("minecraft", "hunger"),
            Identifier.of("minecraft", "levitation"),
            Identifier.of("minecraft", "unluck")
    };

    private static final Identifier[] COOL_EFFECTS = {
            Identifier.of("minecraft", "speed"),
            Identifier.of("minecraft", "strength"),
            Identifier.of("minecraft", "regeneration")
    };

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        return null;
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        Collection<StatusEffectInstance> activeEffects = mc.player.getActiveStatusEffects().values();
        Set<String> activeIds = new HashSet<>();
        activeEffects.forEach(e -> activeIds.add(e.getTranslationKey()));

        // Обновление прогресса анимаций
        activeIds.forEach(id -> {
            float currentAnim = animMap.getOrDefault(id, 0f);
            animMap.put(id, currentAnim + (1f - currentAnim) * 0.15f);
        });

        animMap.keySet().forEach(id -> {
            if (!activeIds.contains(id)) {
                float currentAnim = animMap.get(id);
                animMap.put(id, currentAnim + (0f - currentAnim) * 0.15f);
            }
        });

        animMap.entrySet().removeIf(e -> e.getValue() < 0.01f && !activeIds.contains(e.getKey()));

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(14f);
        float p = scaled(4.5f);
        float fontSize = scaled(7f);
        float round = h * 0.25f;

        String title = "Potions";

        // Расчет максимальной ширины с учетом исчезающих эффектов
        float maxW = getMediumFont().getWidth(title, fontSize) + p * 6f;
        for (String id : animMap.keySet()) {
            if (animMap.get(id) < 0.05f) continue;
            StatusEffectInstance effect = activeEffects.stream().filter(e -> e.getTranslationKey().equals(id)).findFirst().orElse(null);

            String level = (effect != null && effect.getAmplifier() > 0) ? " " + (effect.getAmplifier() + 1) : "";
            String name = Language.getInstance().get(id) + level;
            String duration = (effect != null) ? TextUtil.getDurationText(effect.getDuration()) : "0:00";

            float totalRowW = getMediumFont().getWidth(name, fontSize) + getMediumFont().getWidth(duration, fontSize) + p * 8f;
            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        // Отрисовка заголовка
        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());
        float titleWidth = getMediumFont().getWidth(title, fontSize);
        getMediumFont().drawGradientText(ms, title,
                renderX + (maxW / 2f) - (titleWidth / 2f),
                currentY + h / 2f - fontSize / 2f,
                fontSize, UIColors.primary(), UIColors.secondary(), maxW / 4f);

        currentY += h + 2.5f;

        // Рендер списка эффектов
        for (String id : animMap.keySet()) {
            float anim = animMap.get(id);
            if (anim <= 0.01f) continue;

            StatusEffectInstance effect = activeEffects.stream()
                    .filter(e -> e.getTranslationKey().equals(id)).findFirst().orElse(null);

            float rowH = h * anim;
            int alpha = (int) (255 * anim);

            // Фон (Blur)
            Color themeBlur = UIColors.widgetBlur();
            Color dynamicBg = new Color(themeBlur.getRed(), themeBlur.getGreen(), themeBlur.getBlue(), (int)(themeBlur.getAlpha() * anim));
            RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, rowH, round, dynamicBg);

            // Если эффект еще существует или анимация не закончилась
            if (anim > 0.05f) {
                String level = (effect != null && effect.getAmplifier() > 0) ? " " + (effect.getAmplifier() + 1) : "";
                String name = Language.getInstance().get(id) + level;
                String durationText = (effect != null) ? TextUtil.getDurationText(effect.getDuration()) : "0:00";

                Color baseTextColor = UIColors.textColor();
                Color effectColor = baseTextColor;

                if (effect != null) {
                    Identifier effectId = effect.getEffectType().getKey().get().getValue();
                    effectColor = isBadEffect(effectId) ? ColorUtil.flashingColor(UIColors.negativeColor(), baseTextColor) :
                            isCoolEffect(effectId) ? ColorUtil.flashingColor(UIColors.positiveColor(), baseTextColor) : baseTextColor;
                }

                Color dynamicText = new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), alpha);
                float textCenterY = currentY + (rowH / 2f) - (fontSize / 2f);

                // Отрисовка названия
                getMediumFont().drawText(ms, name, renderX + p + 2f, textCenterY, fontSize, dynamicText);

                // Отрисовка плашки времени
                float durTextW = getMediumFont().getWidth(durationText, fontSize);
                float boxW = durTextW + scaled(9);
                float boxH = (fontSize + scaled(2.5f)) * anim;
                float boxX = renderX + maxW - p - boxW;
                float boxY = currentY + (rowH / 2f) - (boxH / 2f);
                Color durationBoxColor = new Color(255, 255, 255, (int)(30 * anim));

                RenderUtil.RECT.draw(ms, boxX, boxY, boxW, boxH, 2.5f, durationBoxColor);
                getMediumFont().drawText(ms, durationText, boxX + (boxW / 2f) - (durTextW / 2f), textCenterY, fontSize, dynamicText);
            }

            currentY += rowH + 1.5f;
        }

        getDraggable().setWidth(maxW);
        getDraggable().setHeight(currentY - y);
    }

    private boolean isBadEffect(Identifier id) {
        for (Identifier badId : BAD_EFFECTS) { if (badId.equals(id)) return true; }
        return false;
    }

    private boolean isCoolEffect(Identifier id) {
        for (Identifier coolId : COOL_EFFECTS) { if (coolId.equals(id)) return true; }
        return false;
    }
}
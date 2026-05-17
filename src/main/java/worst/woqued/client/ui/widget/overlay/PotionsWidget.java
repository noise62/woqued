package worst.woqued.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import worst.woqued.api.utils.color.ColorUtil;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.other.TextUtil;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.ui.widget.ContainerWidget;

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

        float h = scaled(12f);
        float p = scaled(4.5f);
        float fontSize = scaled(6f);
        float round = h * 0.25f;
        float iconPadding = scaled(5f);

        String title = "Potions";
        String icon = "u"; // Иконка для зелий

        // Расчет максимальной ширины
        float titleWidth = getMediumFont().getWidth(title, fontSize);
        float iconWidth = Fonts.WOQUED.getWidth(icon, fontSize);
        float totalTitleContentWidth = titleWidth + iconPadding + iconWidth;

        float maxW = totalTitleContentWidth + p * 2f;

        for (String id : animMap.keySet()) {
            if (animMap.get(id) < 0.05f) continue;
            StatusEffectInstance effect = activeEffects.stream().filter(e -> e.getTranslationKey().equals(id)).findFirst().orElse(null);

            String level = (effect != null && effect.getAmplifier() > 0) ? " " + (effect.getAmplifier() + 1) : "";
            String name = Language.getInstance().get(id) + level;
            String duration = (effect != null) ? TextUtil.getDurationText(effect.getDuration()) : "0:00";

            float nameW = getMediumFont().getWidth(name, fontSize);
            float durationW = getMediumFont().getWidth(duration, fontSize);
            float totalRowW = nameW + durationW + p * 4f;
            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        // --- РЕНДЕР ЗАГОЛОВКА (как в Keybinds) ---
        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());

        float centerY = currentY + h / 2f - fontSize / 2f;
        float currentX = renderX + p;

        // Заголовок слева
        getMediumFont().drawText(ms, title, currentX, centerY, fontSize, UIColors.textColor());

        // Значок справа
        float iconX = renderX + maxW - p - iconWidth;
        Fonts.WOQUED.drawGradientText(ms, icon, iconX, centerY,
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

            float textCenterY = currentY + (rowH / 2f) - (fontSize / 2f);

            if (anim > 0.05f) {
                String level = (effect != null && effect.getAmplifier() > 0) ? " " + (effect.getAmplifier() + 1) : "";
                String name = Language.getInstance().get(id) + level;
                String durationText = (effect != null) ? TextUtil.getDurationText(effect.getDuration()) : "0:00";

                // Цвет текста в зависимости от эффекта
                Color baseTextColor = UIColors.textColor();
                Color effectColor = baseTextColor;

                if (effect != null) {
                    Identifier effectId = effect.getEffectType().getKey().get().getValue();
                    effectColor = isBadEffect(effectId) ? ColorUtil.flashingColor(UIColors.negativeColor(), baseTextColor) :
                            isCoolEffect(effectId) ? ColorUtil.flashingColor(UIColors.positiveColor(), baseTextColor) : baseTextColor;
                }

                Color dynamicText = new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), alpha);

                // Цвета для ректов
                Color nameRectColor = UIColors.widgetBlur();
                Color durationRectColor = UIColors.widgetBlur();

                // ===== НАЗВАНИЕ ЭФФЕКТА со своим ректом =====
                float nameTextW = getMediumFont().getWidth(name, fontSize);
                float nameRectW = nameTextW + scaled(9);
                float nameRectH = (fontSize + scaled(6f)) * anim;
                float nameRectX = renderX;
                float nameRectY = currentY + (rowH / 2f) - (nameRectH / 2f);

                RenderUtil.RECT.draw(ms, nameRectX, nameRectY, nameRectW, nameRectH, 2.5f, nameRectColor);
                getMediumFont().drawText(ms, name,
                        nameRectX + (nameRectW / 2f) - (nameTextW / 2f),
                        textCenterY, fontSize, dynamicText);

                // ===== ДЛИТЕЛЬНОСТЬ со своим ректом (после названия) =====
                float durTextW = getMediumFont().getWidth(durationText, fontSize);
                float durRectW = durTextW + scaled(9);
                float durRectH = (fontSize + scaled(6f)) * anim;
                float gapAfterName = scaled(2f); // Отступ после названия
                float durRectX = nameRectX + nameRectW + gapAfterName;
                float durRectY = currentY + (rowH / 2f) - (durRectH / 2f);

                RenderUtil.RECT.draw(ms, durRectX, durRectY, durRectW, durRectH, 2.5f, durationRectColor);
                getMediumFont().drawText(ms, durationText,
                        durRectX + (durRectW / 2f) - (durTextW / 2f),
                        textCenterY, fontSize, dynamicText);
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
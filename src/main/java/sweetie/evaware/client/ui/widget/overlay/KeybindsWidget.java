package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.system.backend.KeyStorage;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.awt.Color;
import java.util.*;

public class KeybindsWidget extends ContainerWidget {
    private final Map<Module, Float> animMap = new HashMap<>();

    public KeybindsWidget() {
        super(3f, 120f);
    }

    @Override
    public String getName() {
        return "Keybinds";
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        return null;
    }

    @Override
    public void render(MatrixStack ms) {
        // Логика анимаций
        ModuleManager.getInstance().getModules().forEach(m -> {
            boolean active = m.isEnabled() && m.hasBind();
            float currentAnim = animMap.getOrDefault(m, 0f);
            animMap.put(m, currentAnim + ((active ? 1f : 0f) - currentAnim) * 0.15f);
        });

        animMap.entrySet().removeIf(e -> e.getValue() < 0.05f && !e.getKey().isEnabled());

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();

        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        // Параметры для единства стиля
        float h = scaled(14f);
        float p = scaled(4.5f);
        float fontSize = scaled(7f);
        float round = h * 0.25f;

        String title = "Keybinds";

        // Расчет максимальной ширины
        float maxW = getMediumFont().getWidth(title, fontSize) + p * 6f;
        for (Map.Entry<Module, Float> e : animMap.entrySet()) {
            if (e.getValue() <= 0.05f) continue;

            String keyName = KeyStorage.getBind(e.getKey().getBind());
            float moduleNameW = getMediumFont().getWidth(e.getKey().getName(), fontSize);
            float keyNameW = getMediumFont().getWidth(keyName, fontSize);

            float totalRowW = moduleNameW + keyNameW + p * 8f;
            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        // --- РЕНДЕР ЗАГОЛОВКА ---
        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());

        // Вычисление центра для текста
        float titleWidth = getMediumFont().getWidth(title, fontSize);
        float titleX = renderX + (maxW / 2f) - (titleWidth / 2f);

        // Заголовок градиентом по центру
        getMediumFont().drawGradientText(ms, title, titleX, currentY + h / 2f - fontSize / 2f,
                fontSize, UIColors.primary(), UIColors.secondary(), maxW / 4f);

        currentY += h + 2.5f;

        // Рендер списка биндов
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (!animMap.containsKey(m)) continue;

            float anim = animMap.get(m);
            if (anim <= 0.05f) continue;

            float rowH = h * anim;
            int alpha = (int) (255 * anim);

            Color themeBlur = UIColors.widgetBlur();
            Color dynamicBg = new Color(themeBlur.getRed(), themeBlur.getGreen(), themeBlur.getBlue(), (int)(themeBlur.getAlpha() * anim));

            Color themeText = UIColors.textColor();
            Color dynamicText = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), alpha);

            Color keyRectColor = new Color(255, 255, 255, (int)(30 * anim));

            // Фон строки
            RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, rowH, round, dynamicBg);

            float textCenterY = currentY + (rowH / 2f) - (fontSize / 2f);

            // Название модуля
            getMediumFont().drawText(ms, m.getName(), renderX + p + 2f, textCenterY, fontSize, dynamicText);

            // Клавиша
            String keyName = KeyStorage.getBind(m.getBind());
            float keyTextW = getMediumFont().getWidth(keyName, fontSize);

            float keyRectW = keyTextW + scaled(9);
            float keyRectH = (fontSize + scaled(2.5f)) * anim;
            float keyRectX = renderX + maxW - p - keyRectW;
            float keyRectY = currentY + (rowH / 2f) - (keyRectH / 2f);

            if (anim > 0.5f) {
                RenderUtil.RECT.draw(ms, keyRectX, keyRectY, keyRectW, keyRectH, 2.5f, keyRectColor);

                getMediumFont().drawText(ms, keyName,
                        keyRectX + (keyRectW / 2f) - (keyTextW / 2f),
                        textCenterY, fontSize, dynamicText);
            }

            currentY += rowH + 1.5f;
        }

        getDraggable().setWidth(maxW);
        getDraggable().setHeight(currentY - y);
    }
}
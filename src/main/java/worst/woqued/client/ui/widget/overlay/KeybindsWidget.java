package worst.woqued.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleManager;
import worst.woqued.api.system.backend.KeyStorage;
import worst.woqued.api.system.configs.BindManager;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.ui.widget.ContainerWidget;

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
        ModuleManager.getInstance().getModules().forEach(m -> {
            int bind = BindManager.getInstance().has(m.getName())
                    ? BindManager.getInstance().getBind(m.getName())
                    : m.getBind();
            boolean active = m.isEnabled() && (m.hasBind() || BindManager.getInstance().has(m.getName()));
            float currentAnim = animMap.getOrDefault(m, 0f);
            animMap.put(m, currentAnim + ((active ? 1f : 0f) - currentAnim) * 0.15f);
        });

        animMap.entrySet().removeIf(e -> e.getValue() < 0.05f && !e.getKey().isEnabled());

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();

        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(12f);
        float p = scaled(4.5f);
        float fontSize = scaled(6f);
        float round = h * 0.25f;
        float iconPadding = scaled(5f);

        String title = "Keybinds";
        String icon = "t";

        float titleWidth = getMediumFont().getWidth(title, fontSize);
        float iconWidth = Fonts.WOQUED.getWidth(icon, fontSize);
        float totalTitleContentWidth = titleWidth + iconPadding + iconWidth;

        float maxW = totalTitleContentWidth + p * 2f;

        for (Map.Entry<Module, Float> e : animMap.entrySet()) {
            if (e.getValue() <= 0.05f) continue;

            Module m = e.getKey();
            int bind = BindManager.getInstance().has(m.getName())
                    ? BindManager.getInstance().getBind(m.getName())
                    : m.getBind();
            String keyName = KeyStorage.getBind(bind);
            float moduleNameW = getMediumFont().getWidth(m.getName(), fontSize);
            float keyNameW = getMediumFont().getWidth(keyName, fontSize);

            float totalRowW = moduleNameW + keyNameW + p * 8f;
            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());

        float centerY = currentY + h / 2f - fontSize / 2f;
        float currentX = renderX + p;

        getMediumFont().drawText(ms, title, currentX, centerY, fontSize, UIColors.textColor());

        float iconX = renderX + maxW - p - iconWidth;
        Fonts.WOQUED.drawGradientText(ms, icon, iconX, centerY,
                fontSize, UIColors.primary(), UIColors.secondary(), maxW / 4f);

        currentY += h + 2.5f;

        for (Module m : ModuleManager.getInstance().getModules()) {
            if (!animMap.containsKey(m)) continue;

            float anim = animMap.get(m);
            if (anim <= 0.05f) continue;

            int bind = BindManager.getInstance().has(m.getName())
                    ? BindManager.getInstance().getBind(m.getName())
                    : m.getBind();

            float rowH = h * anim;
            int alpha = (int) (255 * anim);

            Color themeText = UIColors.textColor();
            Color dynamicText = new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(), alpha);

            Color moduleRectColor = UIColors.widgetBlur();
            Color keyRectColor = UIColors.widgetBlur();

            float textCenterY = currentY + (rowH / 2f) - (fontSize / 2f);

            String moduleName = m.getName();
            float moduleTextW = getMediumFont().getWidth(moduleName, fontSize);

            float moduleRectW = moduleTextW + scaled(9);
            float moduleRectH = (fontSize + scaled(6f)) * anim;
            float moduleRectX = renderX + p + 0.5f - scaled(4.5f);
            float moduleRectY = currentY + (rowH / 2f) - (moduleRectH / 2f);

            if (anim > 0.05f) {
                RenderUtil.RECT.draw(ms, moduleRectX, moduleRectY, moduleRectW, moduleRectH, 2.5f, moduleRectColor);

                getMediumFont().drawText(ms, moduleName,
                        moduleRectX + (moduleRectW / 2f) - (moduleTextW / 2f),
                        textCenterY, fontSize, dynamicText);
            }

            String keyName = KeyStorage.getBind(bind);
            float keyTextW = getMediumFont().getWidth(keyName, fontSize);

            float keyRectW = keyTextW + scaled(9);
            float keyRectH = (fontSize + scaled(6f)) * anim;
            float gapAfterModule = scaled(2f);
            float keyRectX = moduleRectX + moduleRectW + gapAfterModule;
            float keyRectY = currentY + (rowH / 2f) - (keyRectH / 2f);

            if (anim > 0.05f) {
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
package worst.woqued.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.other.TextUtil;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;

public class CooldownsWidget extends ContainerWidget {
    private final Map<Identifier, Float> animMap = new HashMap<>();

    public CooldownsWidget() {
        super(3f, 100f);
    }

    @Override
    public String getName() {
        return "Cooldowns";
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        return null;
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);

        Set<Identifier> activeGroups = new HashSet<>();
        Map<Identifier, String> nameMap = new HashMap<>();

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (manager.isCoolingDown(stack)) {
                Identifier groupId = manager.getGroup(stack);
                activeGroups.add(groupId);
                nameMap.put(groupId, stack.getItem().getName().getString());
            }
        }

        activeGroups.forEach(id -> {
            float currentAnim = animMap.getOrDefault(id, 0f);
            animMap.put(id, currentAnim + (1f - currentAnim) * 0.15f);
        });

        animMap.keySet().forEach(id -> {
            if (!activeGroups.contains(id)) {
                float currentAnim = animMap.get(id);
                animMap.put(id, currentAnim + (0f - currentAnim) * 0.15f);
            }
        });

        animMap.entrySet().removeIf(e -> e.getValue() < 0.01f && !activeGroups.contains(e.getKey()));

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(12f);
        float p = scaled(4.5f);
        float fontSize = scaled(6f);
        float round = h * 0.25f;
        float iconPadding = scaled(5f);

        String title = "Cooldowns";
        String icon = "c"; // Иконка для кулдаунов

        // Расчет максимальной ширины
        float titleWidth = getMediumFont().getWidth(title, fontSize);
        float iconWidth = Fonts.WOQUED.getWidth(icon, fontSize);
        float totalTitleContentWidth = titleWidth + iconPadding + iconWidth;

        float maxW = totalTitleContentWidth + p * 2f;

        for (Identifier id : animMap.keySet()) {
            if (animMap.get(id) < 0.05f) continue;
            String name = nameMap.getOrDefault(id, "Unknown");
            int remaining = getRemainingCooldownTicks(id, tickDelta);
            String time = TextUtil.getDurationText(remaining);

            float nameW = getMediumFont().getWidth(name, fontSize);
            float timeW = getMediumFont().getWidth(time, fontSize);
            float totalRowW = nameW + timeW + p * 4f;
            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        // Рендер заголовка
        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());

        float centerY = currentY + h / 2f - fontSize / 2f;
        float currentX = renderX + p;

        getMediumFont().drawText(ms, title, currentX, centerY, fontSize, UIColors.textColor());

        float iconX = renderX + maxW - p - iconWidth;
        Fonts.WOQUED.drawGradientText(ms, icon, iconX, centerY,
                fontSize, UIColors.primary(), UIColors.secondary(), maxW / 4f);

        currentY += h + 2.5f;

        // Рендер списка кулдаунов
        for (Identifier id : animMap.keySet()) {
            float anim = animMap.get(id);
            if (anim <= 0.01f) continue;

            float rowH = h * anim;
            int alpha = (int) (255 * anim);

            float textCenterY = currentY + (rowH / 2f) - (fontSize / 2f);

            if (anim > 0.05f) {
                String name = nameMap.getOrDefault(id, "Unknown");
                int remaining = getRemainingCooldownTicks(id, tickDelta);
                String durationText = TextUtil.getDurationText(remaining);

                Color textColor = UIColors.textColor();
                Color dynamicText = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha);

                // Рект для названия
                float nameTextW = getMediumFont().getWidth(name, fontSize);
                float nameRectW = nameTextW + scaled(9);
                float nameRectH = (fontSize + scaled(6f)) * anim;
                float nameRectX = renderX;
                float nameRectY = currentY + (rowH / 2f) - (nameRectH / 2f);

                RenderUtil.RECT.draw(ms, nameRectX, nameRectY, nameRectW, nameRectH, 2.5f, UIColors.widgetBlur());
                getMediumFont().drawText(ms, name,
                        nameRectX + (nameRectW / 2f) - (nameTextW / 2f),
                        textCenterY, fontSize, dynamicText);

                // Рект для времени
                float timeTextW = getMediumFont().getWidth(durationText, fontSize);
                float timeRectW = timeTextW + scaled(9);
                float timeRectH = (fontSize + scaled(6f)) * anim;
                float gapAfterName = scaled(2f);
                float timeRectX = nameRectX + nameRectW + gapAfterName;
                float timeRectY = currentY + (rowH / 2f) - (timeRectH / 2f);

                Color durationBoxColor = new Color(255, 255, 255, (int)(40 * anim));
                RenderUtil.RECT.draw(ms, timeRectX, timeRectY, timeRectW, timeRectH, 2.5f, durationBoxColor);
                getMediumFont().drawText(ms, durationText,
                        timeRectX + (timeRectW / 2f) - (timeTextW / 2f),
                        textCenterY, fontSize, dynamicText);
            }

            currentY += rowH + 1.5f;
        }

        getDraggable().setWidth(maxW);
        getDraggable().setHeight(currentY - y);
    }

    private int getRemainingCooldownTicks(Identifier groupId, float tickDelta) {
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        ItemCooldownManager.Entry entry = manager.entries.get(groupId);

        if (entry != null) {
            return Math.max(0, entry.endTick() - (manager.tick + (int) tickDelta));
        }
        return 0;
    }
}
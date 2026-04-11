package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;

public class CooldownsWidget extends ContainerWidget {
    // Используем Map для анимаций (ID группы кулдауна -> прогресс 0.0-1.0)
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
        return null; // Не используется, так как переопределен render
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);

        // 1. Собираем активные кулдауны и их названия
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

        // 2. Обновление анимаций (точно как в PotionsWidget)
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

        // 3. Параметры геометрии
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(14f);
        float p = scaled(4.5f);
        float fontSize = scaled(7f);
        float round = h * 0.25f;

        String title = "Cooldowns";

        // 4. Расчет максимальной ширины (с учетом активных анимаций)
        float maxW = getMediumFont().getWidth(title, fontSize) + p * 6f;
        for (Identifier id : animMap.keySet()) {
            if (animMap.get(id) < 0.05f) continue;

            String name = nameMap.getOrDefault(id, "Unknown");
            int remaining = getRemainingCooldownTicks(id, tickDelta);
            String time = TextUtil.getDurationText(remaining);

            float totalRowW = getMediumFont().getWidth(name, fontSize) + getMediumFont().getWidth(time, fontSize) + p * 8f;
            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        // 5. Отрисовка заголовка
        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());
        float titleWidth = getMediumFont().getWidth(title, fontSize);
        getMediumFont().drawGradientText(ms, title,
                renderX + (maxW / 2f) - (titleWidth / 2f),
                currentY + h / 2f - fontSize / 2f,
                fontSize, UIColors.primary(), UIColors.secondary(), maxW / 4f);

        currentY += h + 2.5f;

        // 6. Рендер списка кулдаунов
        for (Identifier id : animMap.keySet()) {
            float anim = animMap.get(id);
            if (anim <= 0.01f) continue;

            float rowH = h * anim;
            int alpha = (int) (255 * anim);

            // Фон строки
            Color themeBlur = UIColors.widgetBlur();
            Color dynamicBg = new Color(themeBlur.getRed(), themeBlur.getGreen(), themeBlur.getBlue(), (int)(themeBlur.getAlpha() * anim));
            RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, rowH, round, dynamicBg);

            if (anim > 0.05f) {
                String name = nameMap.getOrDefault(id, "Unknown");
                int remaining = getRemainingCooldownTicks(id, tickDelta);
                String durationText = TextUtil.getDurationText(remaining);

                Color textColor = UIColors.textColor();
                Color dynamicText = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha);
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

    private int getRemainingCooldownTicks(Identifier groupId, float tickDelta) {
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        // Используем доступ к entries как в вашем оригинальном файле
        ItemCooldownManager.Entry entry = manager.entries.get(groupId);

        if (entry != null) {
            return Math.max(0, entry.endTick() - (manager.tick + (int) tickDelta));
        }
        return 0;
    }
}
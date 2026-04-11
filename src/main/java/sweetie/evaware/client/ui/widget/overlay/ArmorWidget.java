package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ArmorWidget extends Widget {
    public ArmorWidget() {
        super(30f, 100f);
    }

    @Override
    public String getName() {
        return "Armor";
    }

    private final List<ItemStack> ITEMS = new ArrayList<>();

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack matrixStack = event.matrixStack();
        DrawContext context = event.context();

        updateItems();
        if (ITEMS.isEmpty()) return;

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float itemSize = scaled(13f);
        float gap = scaled(3f);

        int screenWidth = mc.getWindow().getScaledWidth();

        float longSide = (itemSize + gap) * 6f + gap;
        float shortSide = itemSize + gap * 2f;

        float threshold = longSide + gap;

        boolean isVertical = x < threshold || x > screenWidth - threshold;

        float currentWidth;
        float currentHeight;

        if (isVertical) {
            currentWidth = shortSide;
            currentHeight = longSide;
        } else {
            currentWidth = longSide;
            currentHeight = shortSide;
        }

        updateDraggable(currentHeight, currentWidth);

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, currentWidth, currentHeight, scaled(5f), UIColors.widgetBlur());

        float currentX = x + gap;
        float currentY = y + gap;

        float scaleFactor = itemSize / 16f;

        for (ItemStack item : ITEMS) {
            matrixStack.push();

            matrixStack.translate(currentX, currentY, 0f);
            matrixStack.scale(scaleFactor, scaleFactor, 1f);

            context.drawItem(item, 0, 0);

            matrixStack.pop();

            matrixStack.push();
            matrixStack.translate(currentX, currentY, 0f);

            float barHeight = scaleFactor * 2f;
            drawBar(matrixStack, item, 0, itemSize - barHeight, itemSize, barHeight, barHeight * 0.7f);

            matrixStack.pop();

            float next = itemSize + gap;
            if (isVertical) currentY += next;
            else currentX += next;

        }
    }

    private void drawBar(MatrixStack matrixStack, ItemStack item, float x, float y, float width, float height, float offset) {
        if (!item.isDamageable()) return;

        float maxDamage = item.getMaxDamage();
        float currentDamage = item.getDamage();
        float progress = (maxDamage - currentDamage) / maxDamage;

        Color color = ColorUtil.interpolate(UIColors.positiveColor(), UIColors.negativeColor(), progress);

        RenderUtil.RECT.draw(matrixStack, x + offset, y,
                (width - offset * 2f) * progress, height,
                height * 0.2f, color);
    }

    private void updateDraggable(float height, float width) {
        getDraggable().setHeight(height);
        getDraggable().setWidth(width);
    }

    private void updateItems() {
        ITEMS.clear();
        if (mc.player == null) return;
        PlayerEntity player = mc.player;

        ITEMS.add(player.getMainHandStack());
        ITEMS.add(player.getOffHandStack());

        List<ItemStack> armor = player.getInventory().armor;
        for (int i = armor.size() - 1; i >= 0; i--) {
            ItemStack stack = armor.get(i);
            ITEMS.add(stack);
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {}
}
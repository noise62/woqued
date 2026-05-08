package worst.woqued.client.ui.widget.overlay;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.util.math.MatrixStack;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.client.ui.widget.Widget;

import java.util.Map;
import java.util.UUID;

public class BossBarWidget extends Widget {

    public BossBarWidget() {
        super(3f, 50f);
    }

    @Override
    public String getName() {
        return "BossBar";
    }

    @Override
    public void render(MatrixStack matrixStack) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        BossBarHud bossBarHud = mc.inGameHud.getBossBarHud();
        if (bossBarHud == null) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        Map<UUID, ClientBossBar> bossBars = bossBarHud.bossBars;
        if (bossBars.isEmpty()) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float gap = getGap();

        float currentY = y;
        float maxWidth = 0f;
        float totalHeight = 0f;

        for (ClientBossBar bossBar : bossBars.values()) {
            float[] dims = renderBossBar(matrixStack, x, currentY, bossBar);
            maxWidth = Math.max(maxWidth, dims[2]);
            totalHeight += dims[3] + gap;
            currentY += dims[3] + gap;
        }

        if (totalHeight > 0) totalHeight -= gap;

        getDraggable().setWidth(maxWidth);
        getDraggable().setHeight(totalHeight);
    }

    private float[] renderBossBar(MatrixStack matrixStack, float x, float y, ClientBossBar bossBar) {
        float fontSize   = scaled(6f);
        float barHeight  = scaled(6f);
        float barWidth   = scaled(120f);
        float gap        = getGap() * 0.8f;

        String name     = bossBar.getName().getString();
        float  progress = bossBar.getPercent();

        float textWidth    = getMediumFont().getWidth(name, fontSize);
        float contentWidth = Math.max(barWidth + gap * 2f, textWidth + gap * 3f);
        float bgHeight     = fontSize + barHeight + gap * 2f;
        float round = bgHeight * 0.3f;

        RenderUtil.RECT.draw(matrixStack, x, y, contentWidth, bgHeight, round, UIColors.widgetBlur());
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, contentWidth, bgHeight, round, UIColors.widgetBlur());

        float textX = x + (contentWidth - textWidth) / 2f;
        float textY = y + gap;
        getMediumFont().drawText(matrixStack, name, textX, textY, fontSize, UIColors.textColor());

        float barX    = x + (contentWidth - barWidth) / 2f;
        float barY    = y + fontSize + gap * 1.5f;
        float barRound = barHeight * 0.3f;
        RenderUtil.RECT.draw(matrixStack, barX, barY, barWidth, barHeight, barRound, UIColors.backgroundBlur());

        float progressWidth = barWidth * progress;
        if (progressWidth > 0.5f) {
            RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY, progressWidth, barHeight, barRound,
                    UIColors.primary(), UIColors.secondary(), UIColors.primary(), UIColors.secondary());
        }

        return new float[]{x, y, contentWidth, bgHeight};
    }
}
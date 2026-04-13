package sweetie.evaware.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.features.modules.combat.AuraModule;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;

public class TargetInfoWidget extends Widget {

    private final AnimationUtil showAnimation = new AnimationUtil();
    private float healthAnimation = 0f;
    private float absorptionAnimation = 0f;
    private float scrollOffset = 0f;
    private LivingEntity target;

    public TargetInfoWidget() {
        super(30f, 30f);
    }

    @Override
    public String getName() {
        return "Target info";
    }

    @Override
    public void render(MatrixStack matrixStack) {
        update();
        LivingEntity currentTarget = getTarget();
        if (currentTarget != null) target = currentTarget;

        if (showAnimation.getValue() <= 0.0 || target == null) return;

        float healthPct = MathHelper.clamp(target.getHealth() / target.getMaxHealth(), 0f, 1f);
        healthAnimation = MathUtil.interpolate(healthAnimation, healthPct, 0.1f);

        float anim = (float) showAnimation.getValue();
        int fullAlpha = (int) (anim * 255f);

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float paddingX = scaled(4f);
        float paddingY = scaled(3f);
        float headSize = scaled(28f);
        float gapHeadText = scaled(5f);
        float barHeight = scaled(6f);
        float barRadius = scaled(1.5f);

        float nicknameHeight = scaled(10f);
        float hpTextHeight = scaled(9f);
        float gapNickHp = scaled(1f);
        float gapHpBar = scaled(2f);
        float textZoneHeight = nicknameHeight + gapNickHp + hpTextHeight + gapHpBar + barHeight;
        float textZoneWidth = scaled(58f);

        float totalWidth = paddingX + headSize + gapHeadText + textZoneWidth + paddingX;
        float totalHeight = paddingY * 2f + Math.max(headSize, textZoneHeight);

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, totalWidth, totalHeight, scaled(7f),
                new Color(25, 25, 30, (int) (255f * anim)));

        float headX = x + paddingX;
        float headY = y + (totalHeight - headSize) / 2f;
        float headRadius = scaled(4f);

        float hurtProgress = target.hurtTime / 10f;
        Color headTint;
        if (hurtProgress > 0f) {
            int red = 255;
            int green = (int) (255f * (1f - hurtProgress));
            int blue = (int) (255f * (1f - hurtProgress));
            headTint = new Color(red, green, blue, fullAlpha);
        } else {
            headTint = ColorUtil.setAlpha(Color.WHITE, fullAlpha);
        }

        if (target instanceof PlayerEntity player) {
            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player, headX, headY, headSize, headSize,
                    0f, headRadius, headTint);
        }

        float absorptionPct = MathHelper.clamp(target.getAbsorptionAmount() / 20f, 0f, 1f);
        absorptionAnimation = MathUtil.interpolate(absorptionAnimation, absorptionPct, 0.1f);

        float contentX = headX + headSize + gapHeadText;
        float contentY = y + (totalHeight - textZoneHeight) / 2f;

        String name = target.getName().getString();
        float nameWidth = getMediumFont().getWidth(name, scaled(9f));

        matrixStack.push();

        double scale = mc.getWindow().getScaleFactor();
        int scissorX = (int) (contentX * scale);
        int scissorY = (int) (mc.getWindow().getFramebufferHeight() - (contentY + nicknameHeight) * scale);
        int scissorW = (int) (textZoneWidth * scale);
        int scissorH = (int) (nicknameHeight * scale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        if (nameWidth > textZoneWidth) {
            float scrollSpeed = 30f;
            float delta = mc.getRenderTickCounter().getTickDelta(true);
            scrollOffset += (scrollSpeed * delta) / 20f;
            float totalScroll = nameWidth + textZoneWidth;
            if (scrollOffset > totalScroll) scrollOffset -= totalScroll;

            float drawX = contentX - scrollOffset;
            getMediumFont().drawText(matrixStack, name, drawX, contentY, scaled(9f),
                    new Color(255, 255, 255, fullAlpha));
            getMediumFont().drawText(matrixStack, name, drawX + totalScroll, contentY, scaled(9f),
                    new Color(255, 255, 255, fullAlpha));
        } else {
            scrollOffset = 0f;
            getMediumFont().drawText(matrixStack, name, contentX, contentY, scaled(9f),
                    new Color(255, 255, 255, fullAlpha));
        }

        RenderSystem.disableScissor();
        matrixStack.pop();

        String hpText;
        float hp = target.getHealth();
        if (hp == (int) hp) {
            hpText = "HP: " + (int) hp + ".0";
        } else {
            hpText = "HP: " + String.format("%.1f", hp);
        }
        float hpTextY = contentY + nicknameHeight + gapNickHp;
        getSemiBoldFont().drawText(matrixStack, hpText, contentX, hpTextY, scaled(8f),
                new Color(190, 190, 200, (int) (fullAlpha * 0.85f)));

        float barX = contentX;
        float barY = hpTextY + hpTextHeight + gapHpBar;
        float barWidth = textZoneWidth;

        RenderUtil.RECT.draw(matrixStack, barX, barY, barWidth, barHeight, barRadius,
                new Color(40, 40, 45, fullAlpha));

        float healthWidth = barWidth * healthAnimation;
        if (healthWidth > 1f) {
            Color barLeft, barRight;

            if (healthPct > 0.6f) {
                barLeft = new Color(30, 140, 60, fullAlpha);
                barRight = new Color(80, 220, 100, fullAlpha);
            } else if (healthPct > 0.3f) {

                barLeft = new Color(200, 120, 30, fullAlpha);
                barRight = new Color(255, 180, 60, fullAlpha);
            } else {

                barLeft = new Color(180, 30, 30, fullAlpha);
                barRight = new Color(255, 70, 70, fullAlpha);
            }

            RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY, healthWidth, barHeight,
                    barRadius, barLeft, barRight, barLeft, barRight);
        }

        if (absorptionAnimation > 0f) {
            float absWidth = barWidth * absorptionAnimation;
            Color absLeft = new Color(180, 140, 20, fullAlpha);
            Color absRight = new Color(255, 215, 50, fullAlpha);
            RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY, absWidth, barHeight,
                    barRadius, absLeft, absRight, absLeft, absRight);
        }

        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(totalHeight);
    }

    private void update() {
        showAnimation.update();
        showAnimation.run(getTarget() != null ? 1.0 : 0.0, 150L, Easing.LINEAR);
    }

    private LivingEntity getTarget() {
        AuraModule aura = AuraModule.getInstance();
        if (aura.isEnabled() && aura.target != null) return aura.target;
        if (mc.currentScreen instanceof ChatScreen) return mc.player;
        return null;
    }
}
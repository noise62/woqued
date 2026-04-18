package sweetie.evaware.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.ScissorUtil;
import sweetie.evaware.client.features.modules.combat.AuraModule;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;

public class TargetInfoWidget extends Widget {

    private final AnimationUtil showAnimation = new AnimationUtil();
    private float healthAnimation = 0f;
    private float interpolatedHealth = 0f;
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
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        update();
        LivingEntity currentTarget = getTarget();
        if (currentTarget != null) target = currentTarget;

        if (showAnimation.getValue() <= 0.0 || target == null) return;

        DrawContext context = event.context();
        MatrixStack matrixStack = event.matrixStack();

        // Интерполяция здоровья
        float targetHp = target.getHealth();
        float maxHp = Math.max(target.getMaxHealth(), 1.0f);

        if (interpolatedHealth <= 0 || targetHp <= 0 || Math.abs(interpolatedHealth - targetHp) < 0.05f) {
            interpolatedHealth = targetHp;
        } else {
            float factor = targetHp < interpolatedHealth ? 0.15f : 0.07f;
            interpolatedHealth = MathHelper.lerp(factor, interpolatedHealth, targetHp);
        }

        float healthPct = MathHelper.clamp(interpolatedHealth / maxHp, 0f, 1f);
        healthAnimation = MathUtil.interpolate(healthAnimation, healthPct, 0.1f);
        // ---------------------------------------------

        float anim = (float) showAnimation.getValue();
        int fullAlpha = (int) (anim * 255f);

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float width = scaled(100f);
        float height = scaled(34f);
        float headSize = height - scaled(6f);
        float spacing = scaled(5f);

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, scaled(5f),
                UIColors.widgetBlur(fullAlpha));

        RenderUtil.RECT.draw(matrixStack, x, y, width, height, scaled(5f),
                new Color(0, 0, 0, (int) (40 * anim)));

        float headX = x + scaled(4f);
        float headY = y + (height - headSize) / 2f;
        float hurtProgress = target.hurtTime / 10f;
        
        Color headTint;
        if (hurtProgress > 0f) {
            headTint = new Color(255, (int) (255f * (1f - hurtProgress)), (int) (255f * (1f - hurtProgress)), fullAlpha);
        } else {
            headTint = ColorUtil.setAlpha(Color.WHITE, fullAlpha);
        }

        if (target instanceof PlayerEntity player) {
            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player, headX, headY, headSize, headSize,
                    0f, scaled(4f), headTint);
        }

        float contentX = headX + headSize + spacing;
        String name = target.getName().getString();

        // Центрирование текста по высоте
        float textYBase = y + spacing;
        
        ScissorUtil.start(matrixStack, contentX, y, width - (headSize + spacing * 2f), height);
        getMediumFont().drawText(matrixStack, name, contentX, textYBase, scaled(7.5f),
                ColorUtil.setAlpha(UIColors.textColor(), fullAlpha));
        
        // Используем интерполированное значение
        String hpText = String.format("HP: %.1f", interpolatedHealth).replace(',', '.');
        getSemiBoldFont().drawText(matrixStack, hpText, contentX, textYBase + scaled(9f), scaled(6.5f),
                ColorUtil.setAlpha(UIColors.textColor(), (int) (fullAlpha * 0.9f)));
        ScissorUtil.stop(matrixStack);

        float barWidth = width - (headSize + spacing * 3f);
        float barY = y + height - spacing - scaled(6f);
        float barHeight = scaled(6f);

        RenderUtil.RECT.draw(matrixStack, contentX, barY, barWidth, barHeight, scaled(2f),
                new Color(35, 35, 35, fullAlpha));

        float hpWidth = barWidth * healthAnimation;
        if (hpWidth > 0.5f) {
            RenderUtil.GRADIENT_RECT.draw(matrixStack, contentX, barY, hpWidth, barHeight,
                    scaled(2f), UIColors.primary(fullAlpha), UIColors.secondary(fullAlpha), 
                    UIColors.primary(fullAlpha), UIColors.secondary(fullAlpha));
        }

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
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

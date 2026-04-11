package sweetie.evaware.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.features.modules.combat.AuraModule;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TargetInfoWidget extends Widget {
    @Override
    public String getName() {
        return "Target Info";
    }

    public TargetInfoWidget() {

        super(135f, 44f);
    }

    private final AnimationUtil showAnimation = new AnimationUtil();

    private float healthAnimation = 0f;
    private float damageAnimation = 0f;

    private LivingEntity target;

    @Override
    public void render(MatrixStack matrixStack) {
        update();
        LivingEntity pretendTarget = getTarget();

        if (pretendTarget != null) {
            target = pretendTarget;
        }

        if (showAnimation.getValue() <= 0.05 || target == null) return;

        float anim = (float) showAnimation.getValue();
        int fullAlpha = (int) (anim * 255f);

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float width = 135f;
        float height = 44f;

        float maxHealth = target.getMaxHealth();
        float currentHealth = MathHelper.clamp(target.getHealth() + target.getAbsorptionAmount(), 0, maxHealth);
        float healthPct = currentHealth / maxHealth;

        healthAnimation = MathUtil.interpolate(healthAnimation, healthPct, 0.2f);
        damageAnimation = MathUtil.interpolate(damageAnimation, healthPct, 0.05f);


        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, 5f, UIColors.widgetBlur(fullAlpha));
        RenderUtil.RECT.draw(matrixStack, x, y, width, height, 5f, new Color(20, 20, 20, (int)(180 * anim)));


        if (fullAlpha > 10) {
            drawTargetModel(matrixStack, x + 20, y + 36, 17, target);
        }


        float textX = x + 42f;
        float textY = y + 5f;
        Color textColor = UIColors.textColor(fullAlpha);

        getSemiBoldFont().drawText(matrixStack, target.getName().getString(), textX, textY, 8.5f, textColor);

        String distText = "Dist: " + String.format("%.1f", mc.player.distanceTo(target));
        getMediumFont().drawText(matrixStack, distText, textX, textY + 9f, 6f, new Color(180, 180, 180, fullAlpha));

        String hpString = String.format("%.1f", currentHealth);
        float hpWidth = getSemiBoldFont().getWidth(hpString, 8f);
        getSemiBoldFont().drawText(matrixStack, hpString, x + width - hpWidth - 5f, textY + 1f, 8f, textColor);


        float barX = textX;
        float barY = y + 21f;
        float barWidth = width - 42f - 5f;
        float barHeight = 6.5f;
        float barRound = 2.5f;

        RenderUtil.RECT.draw(matrixStack, barX, barY, barWidth, barHeight, barRound, new Color(40, 40, 40, fullAlpha));

        if (damageAnimation > healthAnimation) {
            RenderUtil.RECT.draw(matrixStack, barX, barY, barWidth * damageAnimation, barHeight, barRound, new Color(255, 255, 255, (int)(150 * anim)));
        }

        Color c1 = UIColors.gradient(0, fullAlpha);
        Color c2 = UIColors.gradient(90, fullAlpha);
        RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY, barWidth * healthAnimation, barHeight, barRound, c1, c2, c1, c2);


        if (target instanceof PlayerEntity player) {
            renderArmor(matrixStack, player, barX, barY + barHeight + 2.5f, fullAlpha);
        }

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
    }

    private void renderArmor(MatrixStack matrices, PlayerEntity player, float x, float y, int alpha) {
        List<ItemStack> items = new ArrayList<>();


        items.add(player.getOffHandStack());


        items.add(player.getMainHandStack());


        for (ItemStack armor : player.getInventory().armor) {
            items.add(armor);
        }





        Collections.reverse(items);

        float itemX = x;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;

            matrices.push();
            matrices.translate(itemX, y, 0);
            matrices.scale(0.65f, 0.65f, 1f);

            DrawContext context = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());

            context.getMatrices().push();
            context.getMatrices().peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
            context.getMatrices().peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());

            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(mc.textRenderer, stack, 0, 0);

            context.getMatrices().pop();
            context.draw();

            matrices.pop();
            itemX += 11f;
        }
    }

    private void drawTargetModel(MatrixStack matrices, float x, float y, int size, LivingEntity entity) {
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf rotationX = new Quaternionf().rotateX((float) (-Math.PI / 6));
        rotation.mul(rotationX);

        float bodyYaw = entity.bodyYaw;
        float yaw = entity.getYaw();
        float pitch = entity.getPitch();
        float prevHeadYaw = entity.prevHeadYaw;
        float headYaw = entity.headYaw;

        entity.bodyYaw = 180.0F;
        entity.setYaw(180.0F);
        entity.setPitch(0.0F);
        entity.headYaw = 180.0F;
        entity.prevHeadYaw = 180.0F;

        DrawContext context = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());

        InventoryScreen.drawEntity(
                context,
                x, y,
                size,
                new Vector3f(0, 0, 0),
                rotation,
                null,
                entity
        );

        context.draw();

        entity.bodyYaw = bodyYaw;
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        entity.prevHeadYaw = prevHeadYaw;
        entity.headYaw = headYaw;
    }

    private void update() {
        showAnimation.update();
        showAnimation.run(getTarget() != null ? 1.0 : 0.0, getDuration(), getEasing());
    }

    private LivingEntity getTarget() {
        AuraModule aura = AuraModule.getInstance();
        if (aura.isEnabled() && aura.target != null) {
            return aura.target;
        }
        if (mc.currentScreen instanceof ChatScreen) {
            return mc.player;
        }
        return null;
    }
}
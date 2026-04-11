package sweetie.evaware.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2f;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ColorSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.system.configs.FriendManager;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.ProjectionUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Font;
import sweetie.evaware.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ModuleRegister(name = "Predictions", category = Category.RENDER)
public class PredictionsModule extends Module {
    @Getter private static final PredictionsModule instance = new PredictionsModule();

    private final MultiBooleanSetting render = new MultiBooleanSetting("Render").value(
            new BooleanSetting("Ender pearl").value(true),
            new BooleanSetting("Trident").value(false),
            new BooleanSetting("Arrow").value(false)
    );
    private final BooleanSetting walls = new BooleanSetting("Through walls").value(true);
    private final BooleanSetting friend = new BooleanSetting("Friendly indicator").value(false);
    private final ColorSetting friendColor = new ColorSetting("Color").value(new Color(0, 255, 0));

    private final List<Points> points = new ArrayList<>();

    private final String UNKNOWN = "Неизвестный";

    public PredictionsModule() {
        addSettings(render, walls, friend, friendColor);
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender3D(event);
        }));

        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender2D(event);
        }));

        addEvents(render3DEvent, render2DEvent);
    }

    private void handleRender2D(Render2DEvent.Render2DEventData event) {
        Font font = Fonts.SF_MEDIUM;
        MatrixStack matrixStack = event.matrixStack();

        for (Points point : points) {
            Vector2f project = ProjectionUtil.project((float)point.position.x, (float)point.position.y, (float)point.position.z);
            if (project.x == Float.MAX_VALUE && project.y == Float.MAX_VALUE) continue;

            String text = String.format("%s (%.1f сек)", point.itemName, point.ticks * 50 / 1000.0);
            String ownerText = "От " + point.ownerName;
            float offset = 3f, fontSize = 7f;
            float textWidth = font.getWidth(text, fontSize);
            float ownerWidth = font.getWidth(ownerText, fontSize);
            float textHeight = fontSize + offset * 2f;
            float addRectWidth = ownerWidth + offset * 2f;
            float posX = project.x - textWidth / 2f - offset;
            float posY = project.y;

            Color bgColor = point.isFriend && friend.getValue() ? friendColor.getValue() : UIColors.backgroundBlur();
            Color textColor = UIColors.textColor();

            RenderUtil.BLUR_RECT.draw(matrixStack, posX, posY, textWidth + offset * 2f, textHeight, 2f, bgColor);
            font.drawText(matrixStack, text, posX + offset, posY + offset, fontSize, textColor);

            if (!ownerText.contains(UNKNOWN)) {
                RenderUtil.BLUR_RECT.draw(matrixStack, project.x - addRectWidth / 2f, posY + textHeight + 2f, addRectWidth, textHeight, 2f, bgColor);
                font.drawText(matrixStack, ownerText, project.x - ownerWidth / 2f, posY + textHeight + 2f + offset, fontSize, textColor);
            }
        }
    }

    private void handleRender3D(Render3DEvent.Render3DEventData event) {
        MatrixStack matrixStack = event.matrixStack();
        Vec3d renderOffset = mc.getEntityRenderDispatcher().camera.getPos();
        points.clear();

        matrixStack.push();
        matrixStack.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (var entity : mc.world.getEntities()) {
            String name = entity instanceof EnderPearlEntity ? ((ThrownItemEntity) entity).getStack().getName().getString() : entity.getName().getString();

            boolean isPearl = entity instanceof EnderPearlEntity;
            boolean isTrident = entity instanceof TridentEntity trident && !trident.isNoClip() && !trident.groundCollision;
            boolean isArrow = entity instanceof ArrowEntity;

            if ((isPearl && render.isEnabled("Ender pearl")) || (isTrident && render.isEnabled("Trident")) || (isArrow && render.isEnabled("Arrow"))) {

                if ((isArrow || isTrident) && (entity.getVelocity().lengthSquared() < 0.001)) continue;

                UUID ownerUuid = ((ProjectileEntity) entity).ownerUuid != null ? ((ProjectileEntity) entity).ownerUuid : null;
                var owner = ownerUuid != null ? mc.world.getPlayerByUuid(ownerUuid) : null;
                boolean isFriend = owner != null && (FriendManager.getInstance().contains(owner.getName().getString()) || owner.getName().getString().equals(mc.getSession().getUsername()));
                String ownerName = owner != null ? mc.getSession().getUsername().equals(owner.getName().getString()) ? "Вас" : owner.getName().getString() : UNKNOWN;

                predictTrajectory(entity, isFriend, name, ownerName, matrixStack);
            }

        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrixStack.pop();
    }


    private void predictTrajectory(Entity entity, boolean isFriend, String itemName, String ownerName, MatrixStack matrixStack) {
        Color color = isFriend && friend.getValue() ? Color.GREEN : UIColors.gradient(entity.age % 360);

        Vec3d motion = entity.getVelocity();
        Vec3d pos = entity.getPos();
        Vec3d prevPos;
        int ticks = 0;

        for (int i = 0; i <= 149; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = getMotion(entity, motion);

            boolean canSee = walls.getValue() || PlayerUtil.canSee(pos);

            var matrix = matrixStack.peek().getPositionMatrix();
            var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            if (canSee) buffer.vertex(matrix, (float)prevPos.x, (float)prevPos.y, (float)prevPos.z)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

            var hit = mc.world.raycast(new RaycastContext(prevPos, pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            if (hit.getType() == HitResult.Type.BLOCK) pos = hit.getPos();

            if (canSee) buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

            if (canSee) BufferRenderer.drawWithGlobalProgram(buffer.end());

            if (hit.getType() == HitResult.Type.BLOCK || pos.y < -128) {
                points.add(new Points(pos, ticks, isFriend, itemName, ownerName));
                break;
            }
            ticks++;
        }
    }


    private Vec3d getMotion(Entity entity, Vec3d motion) {
        Vec3d motion2 = motion;
        motion2 = entity.isTouchingWater() ? motion2.multiply(0.8) : motion2.multiply(0.99);

        if (!entity.hasNoGravity()) {
            motion2 = motion2.subtract(0, 0.03, 0);
        }

        return motion2;
    }

    private record Points(Vec3d position, int ticks, boolean isFriend, String itemName, String ownerName) {}
}

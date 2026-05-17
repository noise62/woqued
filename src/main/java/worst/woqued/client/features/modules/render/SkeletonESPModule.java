package worst.woqued.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.utils.color.UIColors;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Skeleton ESP", category = Category.RENDER)
public class SkeletonESPModule extends Module {
    @Getter private static final SkeletonESPModule instance = new SkeletonESPModule();
    private final BooleanSetting renderSelf = new BooleanSetting("Self").value(false);
    public SkeletonESPModule() {
        addSettings(renderSelf);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            MatrixStack ms = event.matrixStack();
            Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

            ms.push();
            ms.translate(-cam.x, -cam.y, -cam.z);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth(2.0f);

            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PlayerEntity p)) continue;
                if (p == mc.player && !renderSelf.getValue()) continue;
                if (p.isInvisible()) continue;

                render(ms, p, event.partialTicks());
            }

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            ms.pop();
        }));
        addEvents(renderEvent);
    }
    private void render(MatrixStack ms, PlayerEntity p, float t) {
        if (p == mc.player && mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        if (p == mc.player && !renderSelf.getValue()) {
            return;
        }

        double x = MathHelper.lerp(t, p.prevX, p.getX());
        double y = MathHelper.lerp(t, p.prevY, p.getY());
        double z = MathHelper.lerp(t, p.prevZ, p.getZ());

        float bodyYaw = MathHelper.lerpAngleDegrees(t, p.prevBodyYaw, p.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(t, p.prevHeadYaw, p.headYaw);
        float pitch = MathHelper.lerp(t, p.prevPitch, p.getPitch());
        float swing = p.limbAnimator.getPos(t);
        float swingAmt = p.limbAnimator.getSpeed(t);
        float handSwing = p.getHandSwingProgress(t);

        boolean elytra = p.isGliding();

        Color c = UIColors.primary();

        for (Vec3d[] bone : getBones(x, y, z, bodyYaw, headYaw, pitch, swing, swingAmt, handSwing, p.getHeight(), elytra, p.isSneaking())) {
            line(ms, bone[0], bone[1], c);
        }
    }
    private List<Vec3d[]> getBones(double x, double y, double z, float bodyYaw, float headYaw, float pitch,
                                   float swing, float swingAmt, float handSwing, float h, boolean elytra, boolean sneak) {
        List<Vec3d[]> bones = new ArrayList<>();

        MatrixStack ms = new MatrixStack();
        ms.translate(x, y, z);

        if (sneak && !elytra) {
            ms.translate(0, 0.125, 0);
        }

        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

        float bodyPitch = 0;
        if (elytra) {
            bodyPitch = 1.57f + pitch / 57.2958f;
        } else if (sneak) {
            bodyPitch = 0.5f;
        }

        if (elytra || sneak) {
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bodyPitch * 57.2958f));
        }

        if (sneak && !elytra) {
            ms.translate(0, -0.13, 0);
        }

        Vec3d base = getPos(ms);

        ms.push();
        ms.translate(0, h * 0.75, 0);
        Vec3d neck = getPos(ms);

        ms.push();
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw - headYaw));
        if (!elytra) {
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        }
        ms.translate(0, h * 0.15, 0);
        Vec3d head = getPos(ms);
        ms.pop();

        swingAmt = Math.min(swingAmt, 1.0f) * 0.5f;

        ms.push();
        ms.translate(0.25, 0, 0);
        Vec3d lShoulder = getPos(ms);

        float lArmRot;
        if (elytra) {
            lArmRot = -0.2f;
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-5));
        } else {
            lArmRot = MathHelper.cos(swing * 0.6662f + (float)Math.PI) * 0.8f * swingAmt;
        }

        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lArmRot * 57.2958f));
        ms.translate(0, -0.25, 0);
        Vec3d lElbow = getPos(ms);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.max(0, lArmRot * 15)));
        ms.translate(0, -0.25, 0);
        Vec3d lHand = getPos(ms);
        ms.pop();

        ms.push();
        ms.translate(-0.25, 0, 0);
        Vec3d rShoulder = getPos(ms);

        float rArmRot;
        if (elytra) {
            rArmRot = -0.2f;
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(5));
        } else {
            rArmRot = MathHelper.cos(swing * 0.6662f) * 0.8f * swingAmt;
        }

        if (handSwing > 0 && !elytra) {
            float swingProgress = 1.0f - handSwing;
            swingProgress = swingProgress * swingProgress;
            float swingRot = MathHelper.sin(swingProgress * (float)Math.PI);

            float headYawDiff = headYaw - bodyYaw;
            float yawFactor = MathHelper.clamp(headYawDiff / 75.0f, -1.0f, 1.0f);

            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swingRot * 15 * yawFactor));
            rArmRot += -swingRot * 0.8f;
        }

        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rArmRot * 57.2958f));
        ms.translate(0, -0.25, 0);
        Vec3d rElbow = getPos(ms);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.max(0, rArmRot * 15)));
        ms.translate(0, -0.25, 0);
        Vec3d rHand = getPos(ms);
        ms.pop();

        ms.pop();

        ms.push();
        ms.translate(0, h * 0.5, 0);
        Vec3d waist = getPos(ms);
        ms.pop();

        ms.push();
        ms.translate(0, h * 0.3, 0);
        Vec3d pelvis = getPos(ms);

        ms.push();
        ms.translate(0.125, 0, 0);
        Vec3d lHip = getPos(ms);

        float lLegRot = MathHelper.cos(swing * 0.6662f) * 0.5f * swingAmt;
        if (elytra) {
            lLegRot = 0.1f;
        }

        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lLegRot * 57.2958f));
        ms.translate(0, -0.25, 0);
        Vec3d lKnee = getPos(ms);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(lLegRot) * 15));
        ms.translate(0, -0.25, 0);
        Vec3d lFoot = getPos(ms);
        ms.pop();

        ms.push();
        ms.translate(-0.125, 0, 0);
        Vec3d rHip = getPos(ms);

        float rLegRot = MathHelper.cos(swing * 0.6662f + (float)Math.PI) * 0.5f * swingAmt;
        if (elytra) {
            rLegRot = 0.1f;
        }

        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rLegRot * 57.2958f));
        ms.translate(0, -0.25, 0);
        Vec3d rKnee = getPos(ms);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(rLegRot) * 15));
        ms.translate(0, -0.25, 0);
        Vec3d rFoot = getPos(ms);
        ms.pop();

        ms.pop();
        ms.pop();

        bones.add(new Vec3d[]{neck, head});
        bones.add(new Vec3d[]{neck, waist});
        bones.add(new Vec3d[]{waist, pelvis});
        bones.add(new Vec3d[]{neck, lShoulder});
        bones.add(new Vec3d[]{neck, rShoulder});
        bones.add(new Vec3d[]{lShoulder, lElbow});
        bones.add(new Vec3d[]{lElbow, lHand});
        bones.add(new Vec3d[]{rShoulder, rElbow});
        bones.add(new Vec3d[]{rElbow, rHand});
        bones.add(new Vec3d[]{pelvis, lHip});
        bones.add(new Vec3d[]{pelvis, rHip});
        bones.add(new Vec3d[]{lHip, lKnee});
        bones.add(new Vec3d[]{lKnee, lFoot});
        bones.add(new Vec3d[]{rHip, rKnee});
        bones.add(new Vec3d[]{rKnee, rFoot});

        return bones;
    }
    private Vec3d getPos(MatrixStack ms) {
        Vector3f pos = ms.peek().getPositionMatrix().transformPosition(0, 0, 0, new Vector3f());
        return new Vec3d(pos.x, pos.y, pos.z);
    }
    private void line(MatrixStack ms, Vec3d start, Vec3d end, Color c) {
        Matrix4f m = ms.peek().getPositionMatrix();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buf.vertex(m, (float) start.x, (float) start.y, (float) start.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        buf.vertex(m, (float) end.x, (float) end.y, (float) end.z).color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
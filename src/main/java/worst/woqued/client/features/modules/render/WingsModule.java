package worst.woqued.client.features.modules.render;

import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.system.configs.FriendManager;
import worst.woqued.api.utils.color.UIColors;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleRegister(name = "Wings", category = Category.RENDER)
public class WingsModule extends Module {
    @Getter
    private static final WingsModule instance = new WingsModule();

    private static final float DEFAULT_SPREAD = 8.0f;
    private static final int DEFAULT_ALPHA = 220;

    private static final WingPoint[] SHAPE = {
            new WingPoint(0.08f, 0.10f, 0.88f),
            new WingPoint(0.28f, 0.34f, 0.78f),
            new WingPoint(0.56f, 0.82f, 0.62f),
            new WingPoint(0.86f, 0.30f, 0.52f),
            new WingPoint(1.14f, 0.46f, 0.40f),
            new WingPoint(1.24f, 0.04f, 0.30f),
            new WingPoint(1.02f, -0.18f, 0.28f),
            new WingPoint(1.18f, -0.64f, 0.22f),
            new WingPoint(0.86f, -0.46f, 0.20f),
            new WingPoint(0.80f, -0.98f, 0.14f),
            new WingPoint(0.54f, -0.74f, 0.16f),
            new WingPoint(0.30f, -1.16f, 0.12f),
            new WingPoint(0.10f, -0.54f, 0.18f)
    };

    private final BooleanSetting self = new BooleanSetting("Self").value(true);
    private final BooleanSetting players = new BooleanSetting("Players").value(false);
    private final BooleanSetting friends = new BooleanSetting("Friends").value(false);
    private final SliderSetting size = new SliderSetting("Size").value(1.0f).range(0.75f, 1.35f).step(0.05f);

    private float selfBodyYaw;
    private boolean selfBodyYawInitialized;

    public WingsModule() {
        addSettings(self, players, friends, size);
    }

    @Override
    public void onDisable() {
        selfBodyYawInitialized = false;
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || mc.gameRenderer == null) {
                return;
            }

            MatrixStack matrixStack = event.matrixStack();
            float partialTicks = event.partialTicks();
            Vec3d camera = mc.gameRenderer.getCamera().getPos();

            matrixStack.push();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            try {
                if (self.getValue()
                        && !mc.options.getPerspective().isFirstPerson()
                        && mc.player.isAlive()
                        && !hasElytra(mc.player)) {
                    renderWings(matrixStack, mc.player, partialTicks, camera);
                }

                if (players.getValue() || friends.getValue()) {
                    for (var entity : mc.world.getEntities()) {
                        if (!(entity instanceof PlayerEntity player) || player == mc.player) {
                            continue;
                        }
                        if (!shouldRenderRemote(player)) {
                            continue;
                        }
                        renderWings(matrixStack, player, partialTicks, camera);
                    }
                }
            } finally {
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SrcFactor.SRC_ALPHA,
                        GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SrcFactor.ONE,
                        GlStateManager.DstFactor.ZERO
                );
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.lineWidth(1f);
                matrixStack.pop();
            }
        }));

        addEvents(renderEvent);
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private boolean shouldRenderRemote(PlayerEntity player) {
        if (!player.isAlive() || player.isInvisible() || player.isSleeping() || hasElytra(player)) {
            return false;
        }

        boolean friendTarget = FriendManager.getInstance().contains(player.getName().getString());
        return friendTarget ? friends.getValue() : players.getValue();
    }

    private void renderWings(MatrixStack matrices, PlayerEntity player, float partialTicks, Vec3d camera) {
        double x = MathHelper.lerp(partialTicks, player.prevX, player.getX()) - camera.x;
        double y = MathHelper.lerp(partialTicks, player.prevY, player.getY()) - camera.y;
        double z = MathHelper.lerp(partialTicks, player.prevZ, player.getZ()) - camera.z;

        float bodyYaw = resolveBodyYaw(player, partialTicks);
        float move = MathHelper.clamp(player.limbAnimator.getSpeed(partialTicks), 0f, 1f);
        WingPose pose = resolvePose(player, partialTicks);
        if (pose == null) {
            return;
        }

        float flap = (float) Math.sin((player.age + partialTicks) * pose.flapSpeed) * pose.flapAmplitude;
        float open = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float wingScale = size.getValue() * pose.scaleMultiplier;

        int baseColor = resolveBaseColor();
        int glowColor = interpolateColor(baseColor, getColor(255, 255, 255, 255), 0.28f);
        int coreColor = interpolateColor(baseColor, getColor(255, 255, 255, 255), 0.55f);

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.translate(0f, 0f, -(player.getWidth() * 0.42f + 0.08f));
        if (pose.preTranslateY != 0f || pose.preTranslateZ != 0f) {
            matrices.translate(0f, pose.preTranslateY, pose.preTranslateZ);
        }
        if (pose.pitchRotation != 0f) {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        }
        if (pose.rollRotation != 0f) {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
        }
        matrices.translate(0f, pose.anchorY, pose.anchorZ);
        matrices.scale(wingScale, wingScale, wingScale);

        renderWingSide(matrices, -1f, open, baseColor, glowColor, coreColor, pose);
        renderWingSide(matrices, 1f, open, baseColor, glowColor, coreColor, pose);
        matrices.pop();
    }

    private void renderWingSide(MatrixStack matrices, float side, float open, int baseColor, int glowColor, int coreColor, WingPose pose) {
        matrices.push();
        matrices.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingLayer(matrices, side, 1.22f, setAlpha(glowColor, (int) (DEFAULT_ALPHA * 0.22f)), setAlpha(glowColor, 0));
        drawWingLayer(matrices, side, 0.84f, setAlpha(coreColor, (int) (DEFAULT_ALPHA * 0.26f)), setAlpha(coreColor, 0));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        drawWingLayer(matrices, side, 1.0f, setAlpha(baseColor, DEFAULT_ALPHA), setAlpha(baseColor, 10));

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawWingOutline(matrices, side, 1.0f, setAlpha(baseColor, (int) (DEFAULT_ALPHA * 0.62f)));
        drawWingRibs(matrices, side, 0.96f, setAlpha(glowColor, (int) (DEFAULT_ALPHA * 0.20f)));
        matrices.pop();
    }

    private void drawWingLayer(MatrixStack matrices, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SHAPE.length; i++) {
            WingPoint current = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            vertex(buffer, matrix, 0f, 0f, 0f, rootColor);
            vertex(buffer, matrix, side * current.x * scale, current.y * scale, 0f, applyPointAlpha(edgeColor, current.alphaMul));
            vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0f, applyPointAlpha(edgeColor, next.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawWingOutline(MatrixStack matrices, float side, float scale, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.lineWidth(1.35f);
        boolean lineSmoothWasEnabled = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (WingPoint point : SHAPE) {
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0f, color);
        }
        vertex(buffer, matrix, side * SHAPE[0].x * scale, SHAPE[0].y * scale, 0f, color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        if (!lineSmoothWasEnabled) {
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }
    }

    private void drawWingRibs(MatrixStack matrices, float side, float scale, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int[] ribIndices = {2, 4, 7, 9, 11};
        RenderSystem.lineWidth(0.9f);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        for (int index : ribIndices) {
            WingPoint point = SHAPE[index];
            vertex(buffer, matrix, 0f, 0f, 0f, setAlpha(color, Math.max(8, (int) (alpha(color) * 0.75f))));
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0f, applyPointAlpha(color, point.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private int resolveBaseColor() {
        java.awt.Color color = UIColors.primary();
        return getColor(color.getRed(), color.getGreen(), color.getBlue(), 255);
    }

    private static int interpolateColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int aa = (a >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int ba = (b >> 24) & 0xFF;
        return getColor(
                (int) (ar + (br - ar) * t),
                (int) (ag + (bg - ag) * t),
                (int) (ab + (bb - ab) * t),
                (int) (aa + (ba - aa) * t)
        );
    }

    private int applyPointAlpha(int color, float multiplier) {
        return setAlpha(color, Math.max(0, Math.min(255, (int) (alpha(color) * multiplier))));
    }

    private static int setAlpha(int color, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int alpha(int color) {
        return (color >> 24) & 0xFF;
    }

    private static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    private static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    private static int getColor(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z)
                .color(red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f);
    }

    private float resolveBodyYaw(PlayerEntity player, float partialTicks) {
        float target = MathHelper.lerpAngleDegrees(partialTicks, player.prevBodyYaw, player.bodyYaw);
        if (player != mc.player) {
            return target;
        }
        if (!selfBodyYawInitialized || player.age < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }
        selfBodyYaw = approachDegrees(selfBodyYaw, target, 14f);
        return selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees(target - current);
        delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    private WingPose resolvePose(PlayerEntity player, float partialTicks) {
        float pitch = MathHelper.lerp(partialTicks, player.prevPitch, player.getPitch());
        boolean swimming = player.isSwimming() || (player.isTouchingWater() && !player.isOnGround());
        if (player.isSleeping()) {
            return null;
        }
        if (player.isGliding()) {
            float flightTicks = (float) player.getGlidingTicks() + partialTicks;
            float flightProgress = MathHelper.clamp(flightTicks * flightTicks / 100f, 0f, 1f);
            float pitchRotation = flightProgress * (-90f - pitch);
            return new WingPose(0.24f, -0.02f, 0.02f, -0.01f, pitchRotation, 0f, 0.72f, 0.92f, 0.10f, 0.58f, 0.03f, 0f, 0.005f, -5f, -2f, 0.13f);
        }
        if (swimming) {
            return new WingPose(0.12f, -0.01f, 0.82f, -0.02f, 48f, 0f, 0.86f, 0.94f, 0.08f, 3.2f, 0.04f, 0f, 0.015f, -9f, -7f, 0.10f);
        }
        if (player.isSneaking()) {
            return new WingPose(0f, -0.005f, 0.92f, -0.02f, 18f, 0f, 1f, 1f, 0.18f, 4.5f, 0.035f, 0f, 0.01f, -11f, -4f, 0.12f);
        }
        return new WingPose(0f, -0.01f, 1.22f, -0.025f, 0f, 0f, 1f, 1f, 0.18f, 4.5f, 0.035f, 0f, 0.01f, -11f, -4f, 0.12f);
    }

    private static final class WingPoint {
        final float x;
        final float y;
        final float alphaMul;

        WingPoint(float x, float y, float alphaMul) {
            this.x = x;
            this.y = y;
            this.alphaMul = alphaMul;
        }
    }

    private static final class WingPose {
        final float preTranslateY;
        final float preTranslateZ;
        final float anchorY;
        final float anchorZ;
        final float pitchRotation;
        final float rollRotation;
        final float openMultiplier;
        final float scaleMultiplier;
        final float motionSpreadBoost;
        final float flapAmplitude;
        final float sideOffset;
        final float sideYOffset;
        final float sideZOffset;
        final float sideRoll;
        final float sidePitch;
        final float flapSpeed;

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                 float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                 float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideYOffset,
                 float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
            this.preTranslateY = preTranslateY;
            this.preTranslateZ = preTranslateZ;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.pitchRotation = pitchRotation;
            this.rollRotation = rollRotation;
            this.openMultiplier = openMultiplier;
            this.scaleMultiplier = scaleMultiplier;
            this.motionSpreadBoost = motionSpreadBoost;
            this.flapAmplitude = flapAmplitude;
            this.sideOffset = sideOffset;
            this.sideYOffset = sideYOffset;
            this.sideZOffset = sideZOffset;
            this.sideRoll = sideRoll;
            this.sidePitch = sidePitch;
            this.flapSpeed = flapSpeed;
        }
    }
}

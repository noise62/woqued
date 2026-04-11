package sweetie.evaware.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.features.modules.render.targetesp.TargetEspMode;

import java.util.ArrayList;
import java.util.List;

public class TargetEspComets extends TargetEspMode {
    private final List<double[]> previousPositions = new ArrayList<>();

    private float ghostRotationAngle = 0f;
    private float ghostYRotationAngle = 0f;
    private float prevGhostRotationAngle = 0f;
    private float prevGhostYRotationAngle = 0f;

    @Override
    public void onUpdate() {
        updateTarget();

        prevGhostRotationAngle = ghostRotationAngle;
        prevGhostYRotationAngle = ghostYRotationAngle;

        ghostRotationAngle += 15f;
        ghostYRotationAngle += 15f;
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        MatrixStack matrixStack = event.matrixStack();

        RenderUtil.WORLD.startRender(matrixStack);

        float size = 0.25f;
        float dony = 1.15f * MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue());
        float rem = (float) (0.4 * MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue()));
        int trailLength = 21;

        List<double[]> currentGhostPositions = new ArrayList<>();

        double centerX = getTargetX();
        double centerY = getTargetY() + currentTarget.getHeight() / 2;
        double centerZ = getTargetZ();

        for (int i = 0; i < 3; i++) {
            float angle = MathUtil.interpolate(prevGhostRotationAngle, ghostRotationAngle) + (i * 180);

            double radius = currentTarget.getWidth() * dony;

            double offsetX = Math.cos(Math.toRadians(angle)) * radius;
            double offsetZ = Math.sin(Math.toRadians(angle)) * radius;
            double offsetY = Math.cos(Math.toRadians(angle)) / 3f * radius;

            double ghostYI = MathUtil.interpolate(prevGhostYRotationAngle, ghostYRotationAngle);

            if (i == 0) {
                offsetY = Math.sin(Math.toRadians(ghostYI)) * currentTarget.getHeight() * rem;
            } else if (i == 2) {
                offsetY = -Math.sin(Math.toRadians(ghostYI)) * currentTarget.getHeight() * rem;
                offsetX = -Math.cos(Math.toRadians(angle)) * radius;
            }

            double ghostX = centerX + offsetX;
            double ghostY = centerY + offsetY;
            double ghostZ = centerZ + offsetZ;

            currentGhostPositions.add(new double[]{ghostX, ghostY, ghostZ, angle});
        }

        previousPositions.addAll(currentGhostPositions);
        if (previousPositions.size() > trailLength * 3) {
            previousPositions.subList(0, previousPositions.size() - trailLength * 3).clear();
        }

        for (int i = 0; i < 3; i++) {
            double[] currentPos = currentGhostPositions.get(i);
            float angle = (float) currentPos[3];

            double renderX = currentPos[0] - mc.getEntityRenderDispatcher().camera.getPos().getX();
            double renderY = currentPos[1] - mc.getEntityRenderDispatcher().camera.getPos().getY();
            double renderZ = currentPos[2] - mc.getEntityRenderDispatcher().camera.getPos().getZ();
            renderGhost(matrixStack, renderX, renderY, renderZ, angle, size, 1);

            for (int t = 0; t < previousPositions.size() / 3; t++) {
                int index = t * 3 + i;
                if (index >= previousPositions.size()) continue;

                double[] trailPos = previousPositions.get(index);
                double trailRenderX = trailPos[0] - mc.getEntityRenderDispatcher().camera.getPos().getX();
                double trailRenderY = trailPos[1] - mc.getEntityRenderDispatcher().camera.getPos().getY();
                double trailRenderZ = trailPos[2] - mc.getEntityRenderDispatcher().camera.getPos().getZ();
                float trailAngle = (float) trailPos[3];

                float trailAlpha = (float) (t + 1) / (previousPositions.size() / 3f + 1);
                float trailSize = Math.max(size * 0.4f, size * trailAlpha);
                renderGhost(matrixStack, trailRenderX, trailRenderY, trailRenderZ, trailAngle, trailSize, trailAlpha);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderUtil.WORLD.endRender(matrixStack);
    }

    public void renderGhost(MatrixStack stack, double x, double y, double z, float angle, float size, float alphaMultiplier) {
        stack.push();
        stack.translate(x, y, z);

        Camera camera = mc.gameRenderer.getCamera();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw() + 180.0F));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch() + 180.0F));

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        Matrix4f matrix = stack.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(UIColors.gradient((int) angle), (int) (255 * showAnimation.getValue())));
        float alpha = c[3] * alphaMultiplier;

        buffer.vertex(matrix, -size, size, 0).texture(0f, 1f).color(c[0], c[1], c[2], alpha);
        buffer.vertex(matrix, size, size, 0).texture(1f, 1f).color(c[0], c[1], c[2], alpha);
        buffer.vertex(matrix, size, -size, 0).texture(1f, 0f).color(c[0], c[1], c[2], alpha);
        buffer.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(c[0], c[1], c[2], alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        stack.pop();
    }
}


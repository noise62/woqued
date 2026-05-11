package worst.woqued.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.system.files.FileUtil;
import worst.woqued.api.utils.animation.Easing;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.client.features.modules.render.targetesp.TargetEspMode;

public class TargetEspCircle extends TargetEspMode {
    @Override
    public void onUpdate() {
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        float alpha = (float) showAnimation.getValue();
        if (alpha <= 0.0F) return;

        float time = (System.currentTimeMillis() % 1000000L) / 1000.0f;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        double x = getTargetX() - cameraPos.x;
        double y = getTargetY() - cameraPos.y;
        double z = getTargetZ() - cameraPos.z;

        float entityWidth = currentTarget.getWidth() * 0.9f;
        float entityHeight = currentTarget.getHeight();
        float animAlpha = Easing.CUBIC_OUT.apply(alpha);

        int baseColor = UIColors.primary((int)(alpha * 255)).getRGB();
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        MatrixStack matrices = event.matrixStack();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        double golovkaY = Math.abs(Math.sin(time * 3.0)) * entityHeight;
        double tailBaseY = Math.abs(Math.sin(time * 3.0 - 0.4)) * entityHeight;

        float golovkaSize = 0.12f;
        float tailSize = 0.08f;

        int totalPoints = 90;
        int tailSegments = 10;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int i = 0; i < totalPoints; i++) {
            double angleRadians = 2 * Math.PI * i / totalPoints;
            float xOffset = (float) (Math.cos(angleRadians) * entityWidth);
            float zOffset = (float) (Math.sin(angleRadians) * entityWidth);

            matrices.push();
            matrices.translate(x + xOffset, y + golovkaY, z + zOffset);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            MatrixStack.Entry entry = matrices.peek();
            int coreAlpha = (int) (animAlpha * 0.9f * 255);

            buffer.vertex(entry.getPositionMatrix(), -golovkaSize / 2, -golovkaSize / 2, 0).texture(0, 0).color(r, g, b, coreAlpha);
            buffer.vertex(entry.getPositionMatrix(), golovkaSize / 2, -golovkaSize / 2, 0).texture(1, 0).color(r, g, b, coreAlpha);
            buffer.vertex(entry.getPositionMatrix(), golovkaSize / 2, golovkaSize / 2, 0).texture(1, 1).color(r, g, b, coreAlpha);
            buffer.vertex(entry.getPositionMatrix(), -golovkaSize / 2, golovkaSize / 2, 0).texture(0, 1).color(r, g, b, coreAlpha);
            matrices.pop();

            for (int t = 1; t <= tailSegments; t++) {
                float tailProgress = (float) t / (tailSegments + 1);
                double currentTailY = golovkaY + (tailBaseY - golovkaY) * tailProgress;
                float currentTailAlpha = animAlpha * (1f - tailProgress) * 0.6f;
                int tailAlpha = (int) (currentTailAlpha * 255);

                matrices.push();
                matrices.translate(x + xOffset, y + currentTailY, z + zOffset);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                MatrixStack.Entry tailEntry = matrices.peek();

                buffer.vertex(tailEntry.getPositionMatrix(), -tailSize / 2, -tailSize / 2, 0).texture(0, 0).color(r, g, b, tailAlpha);
                buffer.vertex(tailEntry.getPositionMatrix(), tailSize / 2, -tailSize / 2, 0).texture(1, 0).color(r, g, b, tailAlpha);
                buffer.vertex(tailEntry.getPositionMatrix(), tailSize / 2, tailSize / 2, 0).texture(1, 1).color(r, g, b, tailAlpha);
                buffer.vertex(tailEntry.getPositionMatrix(), -tailSize / 2, tailSize / 2, 0).texture(0, 1).color(r, g, b, tailAlpha);
                matrices.pop();
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}

package sweetie.evaware.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.client.features.modules.render.targetesp.TargetEspMode;

public class TargetEspTexture extends TargetEspMode {
    private float espValue = 1f;
    private float prevEspValue = 0f;
    private float espSpeed = 1f;
    private boolean flip = false;
    
    @Override
    public void onUpdate() {
        if (currentTarget == null || !canDraw()) return;

        prevEspValue = espValue;
        if (showAnimation.getValue() > 0.8) {
            espValue += espSpeed;
            if (espSpeed > 25) flip = true;
            if (espSpeed < -25) flip = false;
        }

        espSpeed = (float) ((flip ? espSpeed - 0.5f : espSpeed + 0.5f) * showAnimation.getValue());
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        var camera = mc.gameRenderer.getCamera();

        double x = getTargetX() - camera.getPos().x;
        double y = getTargetY() - camera.getPos().y;
        double z = getTargetZ() - camera.getPos().z;

        double size = MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue());

        var matrices = new MatrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(x, (y + currentTarget.getHeight() / 2), z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathUtil.interpolate(prevEspValue, espValue)));
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, FileUtil.getImage("target/rounded_target_esp"));
        matrices.translate(-size / 2, -size / 2, -0.01);
        var matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        int alpha = (int) (showAnimation.getValue() * 255);
        int color1 = UIColors.primary(alpha).getRGB();
        int color2 = UIColors.secondary(alpha).getRGB();

        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, 0f, (float) size, 0f).texture(0f, 1f).color(color1);
        buffer.vertex(matrix, (float) size, (float) size, 0f).texture(1f, 1f).color(color2);
        buffer.vertex(matrix, (float) size, 0f, 0f).texture(1f, 0f).color(color1);
        buffer.vertex(matrix, 0f, 0f, 0f).texture(0f, 0f).color(color2);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}

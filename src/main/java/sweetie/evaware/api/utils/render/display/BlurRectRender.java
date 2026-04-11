package sweetie.evaware.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.render.KawaseBlurProgram;
import sweetie.evaware.client.features.modules.render.InterfaceModule;

import java.awt.*;

public class BlurRectRender implements QuickImports {
    private static final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/blurred_rect"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color color, float mix) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, color, color, color, mix);
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, radius, color, InterfaceModule.getGlassy());
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        draw(matrixStack, x, y, width, height, radius, color, color, color, color, InterfaceModule.getGlassy());
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        draw(matrixStack, x, y, width, height, radius, topLeft, topRight, bottomLeft, bottomRight, InterfaceModule.getGlassy());
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight, float mix) {
        float z = 0f;
        float[] color1 = ColorUtil.normalize(topLeft);
        float[] color2 = ColorUtil.normalize(bottomLeft);
        float[] color3 = ColorUtil.normalize(bottomRight);
        float[] color4 = ColorUtil.normalize(topRight);

        //float a = Math.max(Math.max(color1[3], color2[3]), Math.max(color3[3], color4[3])); // колхоз
        float a = (color1[3] + color2[3] + color3[3] + color4[3]) / 4f; // средние арифмитическое

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Framebuffer fbo = KawaseBlurProgram.fbos.getFirst();

        float smoothness = 0.8f;
        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(0, fbo.getColorAttachment());
        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uSize").set(width, height);
        shader.getUniform("uRadius").set(radius.x, radius.z, radius.w, radius.y);
        shader.getUniform("uMix").set(mix);
        shader.getUniform("uSmoothness").set(smoothness);
        shader.getUniform("uAlpha").set(a);
        shader.getUniform("uTopLeftColor").set(color1[0], color1[1], color1[2], color1[3]);
        shader.getUniform("uBottomLeftColor").set(color2[0], color2[1], color2[2], color2[3]);
        shader.getUniform("uBottomRightColor").set(color3[0], color3[1], color3[2], color3[3]);
        shader.getUniform("uTopRightColor").set(color4[0], color4[1], color4[2], color4[3]);

        float uLeft, uRight, vTop, vBottom;

        if (mix != 1) {
            int texW = fbo.textureWidth;
            int texH = fbo.textureHeight;
            double scale = (double) texW / mc.getWindow().getScaledWidth();
            float fx = (float) (x * scale);
            float fy = (float) (y * scale);
            float fw = (float) (width * scale);
            float fh = (float)( height * scale);
            uLeft = fx / (float) texW;
            uRight = (fx + fw) / (float) texW;
            vTop = 1f - (fy / (float) texH);
            vBottom = 1f - ((fy + fh) / (float) texH);
        } else {
            uLeft = uRight = vTop = vBottom = 0f;
        }

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, z).texture(uLeft, vTop).color(topLeft.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, z).texture(uLeft, vBottom).color(bottomLeft.getRGB());
        builder.vertex(matrix4f,adjustedX + adjustedWidth,adjustedY + adjustedHeight, z).texture(uRight, vBottom).color(bottomRight.getRGB());
        builder.vertex(matrix4f,adjustedX + adjustedWidth, adjustedY, z).texture(uRight, vTop).color(topRight.getRGB());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}

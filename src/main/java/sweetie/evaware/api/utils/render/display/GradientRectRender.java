package sweetie.evaware.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.color.ColorUtil;

import java.awt.*;

public class GradientRectRender implements QuickImports {
    private static final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/gradient_rect"), VertexFormats.POSITION_COLOR, Defines.EMPTY);

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius), topLeft, topRight, bottomLeft, bottomRight);
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        float z = 0f;
        float smoothness = 1f;
        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        float[] color1 = ColorUtil.normalize(topLeft);
        float[] color2 = ColorUtil.normalize(bottomLeft);
        float[] color3 = ColorUtil.normalize(bottomRight);
        float[] color4 = ColorUtil.normalize(topRight);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uSize").set(width, height);
        shader.getUniform("uRadius").set(radius.x, radius.z, radius.w, radius.y);
        shader.getUniform("uSmoothness").set(smoothness);
        shader.getUniform("uTopLeftColor").set(color1[0], color1[1], color1[2], color1[3]);
        shader.getUniform("uBottomLeftColor").set(color2[0], color2[1], color2[2], color2[3]);
        shader.getUniform("uBottomRightColor").set(color3[0], color3[1], color3[2], color3[3]);
        shader.getUniform("uTopRightColor").set(color4[0], color4[1], color4[2], color4[3]);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, z).color(topLeft.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, z).color(bottomLeft.getRGB());
        builder.vertex(matrix4f,adjustedX + adjustedWidth,adjustedY + adjustedHeight, z).color(bottomRight.getRGB());
        builder.vertex(matrix4f,adjustedX + adjustedWidth, adjustedY, z).color(topRight.getRGB());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}

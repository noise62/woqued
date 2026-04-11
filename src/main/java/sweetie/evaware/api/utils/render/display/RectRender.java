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

import java.awt.*;

public class RectRender {
    private static final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/rect"), VertexFormats.POSITION_COLOR, Defines.EMPTY);

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color);
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

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

        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uSize").set(width, height);
        shader.getUniform("uRadius").set(radius.x, radius.z, radius.w, radius.y);
        shader.getUniform("uSmoothness").set(smoothness);

        float z = 0f;
        int colorInt = color.getRGB();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, z).color(colorInt);
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, z).color(colorInt);
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, z).color(colorInt);
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, z).color(colorInt);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}

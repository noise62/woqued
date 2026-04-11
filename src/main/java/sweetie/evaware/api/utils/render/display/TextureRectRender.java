package sweetie.evaware.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.render.RenderUtil;

import java.awt.*;

public class TextureRectRender implements QuickImports {
    private static final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("rect/texture_rect"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    public void drawHead(MatrixStack matrixStack, PlayerEntity player, float x, float y, float width, float height, float gap, float radius, Color color) {
        Identifier skin = ((AbstractClientPlayerEntity) player).getSkinTextures().texture();
        float u = 8f / 64f;
        float u2 = 40f / 64f;

        float superGap = gap * 2f;
        int texture = mc.getTextureManager().getTexture(skin).getGlId();
        RenderUtil.TEXTURE_RECT.draw(matrixStack, x + gap, y + gap, width - superGap, height - superGap, radius, color, u, u, u, u, texture);
        RenderUtil.TEXTURE_RECT.draw(matrixStack, x, y, width, height, radius, color, u2, u, u, u, texture);
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, float radius, Color color, float u, float v, float texWidth, float texHeight, int texture) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, u, v, texWidth, texHeight, texture);
    }

    public void draw(MatrixStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color, float u, float v, float texWidth, float texHeight, int texture) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        float z = 0f;
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

        RenderSystem.setShaderTexture(0, texture);

        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uSize").set(width, height);
        shader.getUniform("uRadius").set(radius.x, radius.z, radius.w, radius.y);
        shader.getUniform("uSmoothness").set(smoothness);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, z).texture(u, v).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, z).texture(u, v + texHeight).color(color.getRGB());
        builder.vertex(matrix4f,adjustedX + adjustedWidth,adjustedY + adjustedHeight, z).texture(u + texWidth, v + texHeight).color(color.getRGB());
        builder.vertex(matrix4f,adjustedX + adjustedWidth, adjustedY, z).texture(u + texWidth, v).color(color.getRGB());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}

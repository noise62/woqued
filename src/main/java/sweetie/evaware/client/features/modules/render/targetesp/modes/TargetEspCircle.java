package sweetie.evaware.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.client.features.modules.render.targetesp.TargetEspMode;
import org.joml.Matrix4f;

public class TargetEspCircle extends TargetEspMode {
    private float circleStep = 0f;

    @Override
    public void onUpdate() {
        updateTarget();
        updateAnimation(400, "In", 1.0f, 1.0f, 1.0f);
        circleStep += 0.12f; // Увеличил шаг для ускорения движения
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        updatePositions();
        
        float partialTicks = event.partialTicks();
        float anim = (float) showAnimation.getValue();
        
        // Плавная интерполяция высоты с учетом partialTicks
        float height = currentTarget.getHeight();
        double smoothStep = circleStep + (0.12f * partialTicks); // Увеличил интерполяцию
        double yOffset = (Math.sin(smoothStep * 0.5) * 0.5 + 0.5) * height; 
        
        double x = getTargetX() - mc.gameRenderer.getCamera().getPos().x;
        double y = getTargetY() - mc.gameRenderer.getCamera().getPos().y + yOffset;
        double z = getTargetZ() - mc.gameRenderer.getCamera().getPos().z;

        MatrixStack ms = event.matrixStack();
        ms.push();
        ms.translate(x, y, z);
        
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        
        int baseColor = UIColors.primary((int)(anim * 255)).getRGB();
        int segments = 64;
        float radius = currentTarget.getWidth() * 0.7f;

        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double xPos = Math.cos(angle) * radius;
            double zPos = Math.sin(angle) * radius;

            Matrix4f matrix = ms.peek().getPositionMatrix();
            buffer.vertex(matrix, (float)xPos, 0f, (float)zPos).color(baseColor);
            buffer.vertex(matrix, (float)xPos, 0.1f, (float)zPos).color(baseColor & 0x00FFFFFF);
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }
}

package sweetie.evaware.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.client.features.modules.render.targetesp.TargetEspMode;

public class TargetEspChains extends TargetEspMode {
    private float movingValue = 0f;

    @Override
    public void onUpdate() {
        updateTarget();
        updateAnimation(400, "In", 1.0f, 1.0f, 1.0f);
        movingValue += 2.0f;
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        updatePositions();
        
        float anim = (float) showAnimation.getValue();
        // Используем partialTicks для плавности анимации
        float smoothMoving = movingValue + (2.0f * event.partialTicks());
        
        double x = getTargetX() - mc.gameRenderer.getCamera().getPos().x;
        double y = getTargetY() - mc.gameRenderer.getCamera().getPos().y;
        double z = getTargetZ() - mc.gameRenderer.getCamera().getPos().z;

        float width = currentTarget.getWidth() * 1.0f;
        
        MatrixStack ms = event.matrixStack();
        ms.push();
        ms.translate(x, y + currentTarget.getHeight() / 2.0f - 0.5f, z);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, FileUtil.getImage("target/chain"));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        int color = UIColors.primary((int)(anim * 255)).getRGB();
        
        for (int chain = 0; chain < 2; chain++) {
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chain == 0 ? 15 : -15));
            ms.translate(chain == 0 ? 0.1f : -0.1f, 0, 0); 
            
            Matrix4f matrix = ms.peek().getPositionMatrix();
            int links = 72;
            for (int i = 0; i < links; i++) {
                float angle = (float) (Math.toRadians(i * (360.0f / links) + smoothMoving));
                float prevAngle = (float) (Math.toRadians((i - 1) * (360.0f / links) + smoothMoving));
                
                float x1 = (float) (Math.cos(prevAngle) * width);
                float z1 = (float) (Math.sin(prevAngle) * width);
                float x2 = (float) (Math.cos(angle) * width);
                float z2 = (float) (Math.sin(angle) * width);

                builder.vertex(matrix, x1, 0, z1).color(color).texture(i / 10f, 0);
                builder.vertex(matrix, x2, 0, z2).color(color).texture((i + 1) / 10f, 0);
                builder.vertex(matrix, x2, 1, z2).color(color).texture((i + 1) / 10f, 1);
                builder.vertex(matrix, x1, 1, z1).color(color).texture(i / 10f, 1);
            }
            ms.pop();
        }
        
        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }
}

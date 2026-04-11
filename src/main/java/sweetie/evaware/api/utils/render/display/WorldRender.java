package sweetie.evaware.api.utils.render.display;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.util.math.MatrixStack;

public class WorldRender {
    public void startRender(MatrixStack matrixStack) {
        matrixStack.push();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
    }

    public void endRender(MatrixStack matrixStack) {
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        matrixStack.pop();
    }
}

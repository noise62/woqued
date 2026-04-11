package sweetie.evaware.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;

import lombok.experimental.UtilityClass;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.system.interfaces.QuickImports;

@UtilityClass
public class ScissorUtil implements QuickImports {
    public void start(MatrixStack matrixStack, float x, float y, float width, float height) {
        float scale = (float) mc.getWindow().getScaleFactor();

        float adjustedY = mc.getWindow().getScaledHeight() - y;

        float scaledX = x * scale;
        float scaledY = adjustedY * scale;
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        matrixStack.push();
        RenderSystem.enableScissor((int)scaledX, (int)(scaledY - scaledHeight), (int)scaledWidth, (int)scaledHeight);
    }

    public void stop(MatrixStack matrixStack) {
        RenderSystem.disableScissor();
        matrixStack.pop();
    }
}

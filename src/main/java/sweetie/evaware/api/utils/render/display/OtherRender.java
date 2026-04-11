package sweetie.evaware.api.utils.render.display;

import net.minecraft.client.util.math.MatrixStack;

public class OtherRender {
    public void scaleStart(MatrixStack matrixStack, float x, float y, double scale) {
        matrixStack.push();
        matrixStack.translate(x, y, 0);
        matrixStack.scale((float) scale, (float) scale, 1f);
        matrixStack.translate(-x, -y, 0);
    }
    public void squshStart(MatrixStack matrixStack, float x, float y, double scale) {
        matrixStack.push();
        matrixStack.translate(x, y, 0);
        matrixStack.scale(1f, (float) scale, 1f);
        matrixStack.translate(-x, -y, 0);
    }

    public void scaleStop(MatrixStack matrixStack) {
        matrixStack.pop();
    }
}

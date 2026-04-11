package sweetie.evaware.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.other.FramebufferResizeEvent;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.framelimiter.FrameLimiter;
import sweetie.evaware.client.features.modules.render.InterfaceModule;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class KawaseBlurProgram implements QuickImports {
    private final ShaderProgramKey upShader = new ShaderProgramKey(FileUtil.getShader("post/blur/upscale"), VertexFormats.POSITION, Defines.EMPTY);
    private final ShaderProgramKey downShader = new ShaderProgramKey(FileUtil.getShader("post/blur/downscale"), VertexFormats.POSITION, Defines.EMPTY);

    private final int blurPasses = InterfaceModule.getPasses();

    public final List<SimpleFramebuffer> fbos = new ArrayList<>();

    private boolean init = false;
    private final FrameLimiter f = new FrameLimiter(false);

    public void load() {
        if (!init) {
            for (int i = 0; i <= InterfaceModule.getPasses(); i++) {
                fbos.add(createFbo());
            }
            init = true;
        }

        FramebufferResizeEvent.getInstance().subscribe(new Listener<>(event -> {
            recreate();
        }));
    }

    public void recreate() {
        fbos.forEach(Framebuffer::delete);
        fbos.clear();

        for (int i = 0; i <= InterfaceModule.getPasses(); i++) {
            fbos.add(createFbo());
        }
    }

    public void render(MatrixStack matrixStack) {
        if (InterfaceModule.getGlassy() != 1f) {
            f.execute(40, () -> {
                int actualPasses = Math.max(fbos.size() - 1, 1);

                applyBlurPass(matrixStack, downShader, mc.getFramebuffer(), fbos.getFirst(), 0);

                for (int i = 0; i < actualPasses; i++) {
                    applyBlurPass(matrixStack, downShader, fbos.get(i), fbos.get(i + 1), i + 1);
                }

                for (int i = actualPasses; i > 0; i--) {
                    applyBlurPass(matrixStack, upShader, fbos.get(i), fbos.get(i - 1), i);
                }

                mc.getFramebuffer().beginWrite(false);
            });
        }
    }

    private void applyBlurPass(MatrixStack matrixStack, ShaderProgramKey shaderKey, Framebuffer source, Framebuffer destination, int pass) {
        destination.beginWrite(false);

        RenderSystem.setShaderTexture(0, source.getColorAttachment());

        ShaderProgram shader = RenderSystem.setShader(shaderKey);

        shader.getUniform("uHalfTexelSize").set(0.5f / (float) source.textureWidth, 0.5f / (float) source.textureHeight);
        shader.getUniform("uOffset").set(InterfaceModule.getOffset() * (pass / (float) blurPasses));

        drawFullQuad(matrixStack.peek().getPositionMatrix());
        destination.endWrite();
    }

    private void drawFullQuad(Matrix4f matrix4f) {
        float width = mc.getWindow().getScaledWidth();
        float height = mc.getWindow().getScaledHeight();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        builder.vertex(matrix4f, 0, 0, 0);
        builder.vertex(matrix4f, 0, height, 0);
        builder.vertex(matrix4f, width, height, 0);
        builder.vertex(matrix4f, width, 0, 0);

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private SimpleFramebuffer createFbo() {
        return new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
    }
}
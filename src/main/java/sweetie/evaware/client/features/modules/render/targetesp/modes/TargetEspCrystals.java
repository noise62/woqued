package sweetie.evaware.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.client.features.modules.render.targetesp.TargetEspMode;

import java.util.ArrayList;
import java.util.List;

public class TargetEspCrystals extends TargetEspMode {
    private final List<Crystal> crystalList = new ArrayList<>();
    private float rotationAngle = 0;
    private net.minecraft.entity.Entity lastRenderedTarget = null;
    private static final Identifier BLOOM_TEX = Identifier.of("evaware", "images/target/bloom.png");

    @Override
    public void onUpdate() {
        updateTarget();
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        if (crystalList.isEmpty() || currentTarget != lastRenderedTarget) {
            createCrystals(currentTarget);
            lastRenderedTarget = currentTarget;
        }

        MatrixStack ms = event.matrixStack();
        float anim = (float) showAnimation.getValue();

        float red = (currentTarget.hurtTime) / 10f;

        RenderSystem.enableDepthTest();
        double x = getTargetX() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = getTargetY() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = getTargetZ() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        rotationAngle = (rotationAngle + 0.5f) % 360;

        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));

        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
        for (Crystal crystal : crystalList) {
            crystal.render(ms, anim, red, camera);
        }
        ms.pop();
        RenderSystem.enableDepthTest();
    }

    private void createCrystals(net.minecraft.entity.Entity target) {
        crystalList.clear();
        crystalList.add(new Crystal(new Vec3d(0, 0.85, 0.8), new Vec3d(-49, 0, 40)));
        crystalList.add(new Crystal(new Vec3d(0.2, 0.85, -0.675), new Vec3d(35, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30, 0, 35)));
        crystalList.add(new Crystal(new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.74, 0.95, -0.4), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30, 0, -25)));
        crystalList.add(new Crystal(new Vec3d(0, 1.35, -0.6), new Vec3d(45, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(0.85, 0.7, 0.1), new Vec3d(-30, 0, 30)));
        crystalList.add(new Crystal(new Vec3d(-0.7, 1.35, -0.3), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.3, 1.35, 0.55), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.7, 0.75, 0), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.2, 0.65, -0.7), new Vec3d(0, 0, 0)));
    }

    private static class Crystal {
        private final Vec3d position;
        private final Vec3d rotation;
        private final float size = 0.06f;
        private final float rotationSpeed;

        public Crystal(Vec3d position, Vec3d rotation) {
            this.position = position;
            this.rotation = rotation;
            this.rotationSpeed = 0.5f + (float)(Math.random() * 1.5f);
        }

        public void render(MatrixStack ms, float anim, float red, net.minecraft.client.render.Camera camera) {
            ms.push();
            ms.translate(position.x, position.y, position.z);
            float pulsation = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.1f);
            ms.scale(pulsation, pulsation, pulsation);

            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));

            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

            int color = UIColors.primary((int) (255 * anim)).getRGB();

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            drawCrystal(ms, color, 0.2f, anim);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            drawCrystal(ms, color, 0.3f, anim);
            drawCrystal(ms, color, 0.8f, anim);

            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            ms.push();
            ms.scale(1.2f, 1.2f, 1.2f);
            drawCrystal(ms, color, 0.3f, anim);
            ms.pop();

            drawBloomSphere(ms, color, anim, camera);
            RenderSystem.depthMask(true);

            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }

        private void drawBloomSphere(MatrixStack ms, int color, float anim, Camera camera) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, BLOOM_TEX);
            int bloomColor = (color & 0x00FFFFFF) | ((int) (0.4f * 25 * anim) << 24);
            float bloomSize = size * 13.0f;

            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f matrix = ms.peek().getPositionMatrix();
            BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bb.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
            bb.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
            bb.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
            bb.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);
            BufferRenderer.drawWithGlobalProgram(bb.end());
            ms.pop();
        }

        private void drawCrystal(MatrixStack ms, int color, float alpha, float anim) {
            BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            int finalColor = (color & 0x00FFFFFF) | ((int) (alpha * 255 * anim) << 24);

            float s = size;
            float h_prism = size * 1f;
            float h_pyramid = size * 1.5f;
            int numSides = 8;

            List<Vec3d> top = new ArrayList<>();
            List<Vec3d> bot = new ArrayList<>();
            for (int i = 0; i < numSides; i++) {
                float angle = (float) (2 * Math.PI * i / numSides);
                float x = (float) (s * Math.cos(angle)), z = (float) (s * Math.sin(angle));
                top.add(new Vec3d(x, h_prism / 2, z));
                bot.add(new Vec3d(x, -h_prism / 2, z));
            }
            Vec3d vTop = new Vec3d(0, h_prism / 2 + h_pyramid, 0);
            Vec3d vBot = new Vec3d(0, -h_prism / 2 - h_pyramid, 0);

            Matrix4f mat = ms.peek().getPositionMatrix();
            for (int i = 0; i < numSides; i++) {
                int next = (i + 1) % numSides;

                drawTriangle(mat, bb, bot.get(i), bot.get(next), top.get(next), finalColor);
                drawTriangle(mat, bb, bot.get(i), top.get(next), top.get(i), finalColor);

                drawTriangle(mat, bb, vTop, top.get(i), top.get(next), finalColor);
                drawTriangle(mat, bb, vBot, bot.get(next), bot.get(i), finalColor);
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());
        }

        private void drawTriangle(Matrix4f mat, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color) {
            bb.vertex(mat, (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            bb.vertex(mat, (float)v2.x, (float)v2.y, (float)v2.z).color(color);
            bb.vertex(mat, (float)v3.x, (float)v3.y, (float)v3.z).color(color);
        }
    }
}

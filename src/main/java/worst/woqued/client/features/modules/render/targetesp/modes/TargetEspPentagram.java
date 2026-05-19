package worst.woqued.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.system.files.FileUtil;
import worst.woqued.api.utils.color.ColorUtil;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.math.MathUtil;
import worst.woqued.client.features.modules.render.targetesp.TargetEspMode;
import worst.woqued.client.features.modules.render.targetesp.TargetEspModule;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class TargetEspPentagram extends TargetEspMode {
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final int TRAIL_MAX = 18;
    private static final float SPARK_FRICTION = 0.93f;
    private static final float SPARK_GRAVITY = 0.00018f;

    private float rotation;
    private float prevRotation;
    private float counterRotation;
    private float prevCounterRotation;
    private float impactProgress;
    private int prevHurtTime;
    private float pulseOffset;
    private float prevPulseOffset;
    private float glitchX;
    private float glitchZ;
    private float glitchDecay;

    @SuppressWarnings("unchecked")
    private final Deque<Float>[] vertexTrails = new Deque[5];

    private static final class Spark {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float size;
        int colorIndex;

        Spark(float x, float y, float vx, float vy, float size, int colorIndex) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = 1.0f;
            this.size = size;
            this.colorIndex = colorIndex;
        }

        void tick() {
            vx *= SPARK_FRICTION;
            vy *= SPARK_FRICTION;
            vy -= SPARK_GRAVITY;
            x += vx;
            y += vy;
            life -= 0.038f;
        }
    }

    private final List<Spark> sparks = new ArrayList<>();

    public TargetEspPentagram() {
        for (int i = 0; i < vertexTrails.length; i++) {
            vertexTrails[i] = new ArrayDeque<>();
        }
    }

    @Override
    public void onUpdate() {
        if (currentTarget == null || !canDraw()) {
            return;
        }

        TargetEspModule module = TargetEspModule.getInstance();
        prevRotation = rotation;
        prevCounterRotation = counterRotation;
        prevPulseOffset = pulseOffset;

        float speed = module.circleSpeed.getValue() * 0.55f;
        rotation += speed;
        counterRotation -= speed * 0.65f;

        updateTrails();
        updateImpactAnimation();
        updateGlitch();
        updateSparks();
    }

    private void updateTrails() {
        for (int i = 0; i < 5; i++) {
            vertexTrails[i].addFirst(rotation + i * 72.0f);
            while (vertexTrails[i].size() > TRAIL_MAX) {
                vertexTrails[i].removeLast();
            }
        }
    }

    private void updateImpactAnimation() {
        TargetEspModule module = TargetEspModule.getInstance();
        if (!module.circleRedOnImpact.getValue() || currentTarget == null) {
            impactProgress = 0.0f;
            pulseOffset = 0.0f;
            prevHurtTime = 0;
            return;
        }

        float fadeIn = module.circleImpactFadeIn.getValue();
        float fadeOut = module.circleImpactFadeOut.getValue();
        float maxIntensity = module.circleImpactIntensity.getValue();
        int hurtTime = currentTarget.hurtTime;

        if (hurtTime > prevHurtTime || (hurtTime > 0 && prevHurtTime == 0)) {
            impactProgress = Math.min(maxIntensity, impactProgress + fadeIn);
            pulseOffset = Math.min(0.22f, pulseOffset + fadeIn * 1.8f);
            triggerGlitch();
            spawnVertexSparks();
        } else if (hurtTime > 0) {
            impactProgress = Math.min(maxIntensity, impactProgress + fadeIn * 0.45f);
            pulseOffset = Math.max(0.0f, pulseOffset - fadeOut * 0.4f);
        } else {
            impactProgress = Math.max(0.0f, impactProgress - fadeOut * 0.62f);
            pulseOffset = Math.max(0.0f, pulseOffset - fadeOut * 0.9f);
        }

        prevHurtTime = hurtTime;
    }

    private void triggerGlitch() {
        float intensity = 0.12f;
        glitchX = (float) (Math.random() * 2.0 - 1.0) * intensity;
        glitchZ = (float) (Math.random() * 2.0 - 1.0) * intensity;
        glitchDecay = 1.0f;
    }

    private void updateGlitch() {
        glitchDecay = Math.max(0.0f, glitchDecay - 0.18f);
        if (glitchDecay <= 0.0f) {
            glitchX = 0.0f;
            glitchZ = 0.0f;
        }
    }

    private void spawnVertexSparks() {
        if (currentTarget == null) {
            return;
        }

        TargetEspModule module = TargetEspModule.getInstance();
        float radius = currentTarget.getWidth() * 0.7f + 0.08f + module.circleSize.getValue() * 0.16f;
        float[][] vertices = starVertices(radius, (float) Math.toRadians(rotation));

        for (int i = 0; i < 5; i++) {
            float px = vertices[i * 2][0];
            float py = vertices[i * 2][1];
            int count = 5 + (int) (Math.random() * 4.0);
            for (int j = 0; j < count; j++) {
                float angle = (float) (Math.random() * TAU);
                float speed = 0.005f + (float) (Math.random() * 0.014f);
                sparks.add(new Spark(
                        px,
                        py,
                        cos(angle) * speed,
                        sin(angle) * speed,
                        0.016f + (float) (Math.random() * 0.024f),
                        i * 40 + (int) (Math.random() * 20.0)
                ));
            }
        }
    }

    private void updateSparks() {
        Iterator<Spark> iterator = sparks.iterator();
        while (iterator.hasNext()) {
            Spark spark = iterator.next();
            spark.tick();
            if (spark.life <= 0.0f) {
                iterator.remove();
            }
        }
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) {
            return;
        }

        MatrixStack matrices = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        TargetEspModule module = TargetEspModule.getInstance();

        double x = getTargetX() - camera.getPos().x + glitchX * glitchDecay;
        double y = getTargetY() - camera.getPos().y + 0.03;
        double z = getTargetZ() - camera.getPos().z + glitchZ * glitchDecay;

        float show = (float) showAnimation.getValue();
        float sizeAnim = MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue());
        float pulse = MathUtil.interpolate(prevPulseOffset, pulseOffset);
        float baseRadius = (currentTarget.getWidth() * 0.7f + 0.08f + module.circleSize.getValue() * 0.16f) * sizeAnim;
        float radius = baseRadius * (1.0f + pulse);
        float rotationValue = MathUtil.interpolate(prevRotation, rotation);
        float counterRotationValue = MathUtil.interpolate(prevCounterRotation, counterRotation);
        float outerLineWidth = 1.8f + pulse * 6.0f;
        float innerLineWidth = 1.1f + pulse * 3.0f;
        float pentagramLineWidth = 1.35f + pulse * 4.5f;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        try {
            renderBloom(matrices, radius * (1.25f + module.circleBloomSize.getValue() * 0.3f), show);

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationValue));
            renderPentagramFill(matrices, radius, show);
            matrices.pop();

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationValue));
            renderCircle(matrices, radius * 1.08f, show, 72, outerLineWidth);
            renderPentagram(matrices, radius, show, pentagramLineWidth);
            matrices.pop();

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(counterRotationValue));
            renderCircleDashed(matrices, radius * 0.72f, show, innerLineWidth);
            matrices.pop();

            renderTrails(matrices, radius, show);
            if (!sparks.isEmpty()) {
                renderSparks(matrices, show);
            }
        } finally {
            RenderSystem.lineWidth(1.0f);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrices.pop();
        }
    }

    private void renderBloom(MatrixStack matrices, float radius, float show) {
        if (!TargetEspModule.getInstance().circleBloom.getValue()) {
            return;
        }

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Color color = blendImpactColor(new Color(228, 240, 255, (int) (72.0f * show)));
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -radius, -radius, 0.0f).texture(0.0f, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, -radius, radius, 0.0f).texture(0.0f, 1.0f).color(color.getRGB());
        buffer.vertex(matrix, radius, radius, 0.0f).texture(1.0f, 1.0f).color(color.getRGB());
        buffer.vertex(matrix, radius, -radius, 0.0f).texture(1.0f, 0.0f).color(color.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderCircle(MatrixStack matrices, float radius, float show, int segments, float lineWidth) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(lineWidth);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float angle1 = TAU * i / segments;
            float angle2 = TAU * (i + 1) / segments;
            Color color1 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(i * 4), (int) (130.0f * show)));
            Color color2 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient((i + 1) * 4), (int) (130.0f * show)));
            buffer.vertex(matrix, cos(angle1) * radius, sin(angle1) * radius, 0.0f).color(color1.getRGB());
            buffer.vertex(matrix, cos(angle2) * radius, sin(angle2) * radius, 0.0f).color(color2.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderCircleDashed(MatrixStack matrices, float radius, float show, float lineWidth) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(lineWidth);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int segments = 48;

        for (int i = 0; i < segments; i++) {
            if (i % 3 == 2) {
                continue;
            }

            float angle1 = TAU * i / segments;
            float angle2 = TAU * (i + 1) / segments;
            Color color1 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(256 - i * 5), (int) (90.0f * show)));
            Color color2 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(256 - (i + 1) * 5), (int) (90.0f * show)));
            buffer.vertex(matrix, cos(angle1) * radius, sin(angle1) * radius, 0.0f).color(color1.getRGB());
            buffer.vertex(matrix, cos(angle2) * radius, sin(angle2) * radius, 0.0f).color(color2.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderPentagramFill(MatrixStack matrices, float radius, float show) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[][] star = starVertices(radius, 0.0f);
        int alpha = Math.min(50, (int) (20.0f * show * (1.0f + impactProgress * 0.7f)));
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < 10; i++) {
            int next = (i + 1) % 10;
            Color color = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(i * 28), alpha));
            buffer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(color.getRGB());
            buffer.vertex(matrix, star[i][0], star[i][1], 0.0f).color(color.getRGB());
            buffer.vertex(matrix, star[next][0], star[next][1], 0.0f).color(color.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderPentagram(MatrixStack matrices, float radius, float show, float lineWidth) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(lineWidth);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float[][] star = starVertices(radius, 0.0f);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < 10; i++) {
            int next = (i + 1) % 10;
            Color color1 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(i * 28), (int) (155.0f * show)));
            Color color2 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(next * 28), (int) (155.0f * show)));
            buffer.vertex(matrix, star[i][0], star[i][1], 0.0f).color(color1.getRGB());
            buffer.vertex(matrix, star[next][0], star[next][1], 0.0f).color(color2.getRGB());
        }

        for (int i = 0; i < 5; i++) {
            int a = i * 2;
            int b = (i * 2 + 4) % 10;
            Color color1 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(i * 54), (int) (190.0f * show)));
            Color color2 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient((i + 2) * 54), (int) (190.0f * show)));
            buffer.vertex(matrix, star[a][0], star[a][1], 0.0f).color(color1.getRGB());
            buffer.vertex(matrix, star[b][0], star[b][1], 0.0f).color(color2.getRGB());
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        float glowSize = radius * 0.09f * (1.0f + pulseOffset * 1.6f);
        for (int i = 0; i < 5; i++) {
            renderPointGlow(matrices, star[i * 2][0], star[i * 2][1], glowSize, show, i * 40);
        }
    }

    private void renderTrails(MatrixStack matrices, float radius, float show) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.6f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int vertex = 0; vertex < 5; vertex++) {
            if (vertexTrails[vertex].size() < 2) {
                continue;
            }

            Float[] angles = vertexTrails[vertex].toArray(new Float[0]);
            boolean hasAny = false;
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (int i = 0; i < angles.length - 1; i++) {
                float progress = 1.0f - (float) i / angles.length;
                float alphaFactor = progress * progress * show * 0.85f;
                if (alphaFactor < 0.01f) {
                    continue;
                }

                float angle1 = (float) Math.toRadians(angles[i] - 90.0f);
                float angle2 = (float) Math.toRadians(angles[i + 1] - 90.0f);
                int colorIndex = vertex * 40 + i * 8;
                Color color1 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(colorIndex), (int) (200.0f * alphaFactor)));
                Color color2 = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(colorIndex + 8), (int) (200.0f * alphaFactor * 0.7f)));
                buffer.vertex(matrix, cos(angle1) * radius, sin(angle1) * radius, 0.0f).color(color1.getRGB());
                buffer.vertex(matrix, cos(angle2) * radius, sin(angle2) * radius, 0.0f).color(color2.getRGB());
                hasAny = true;
            }

            if (hasAny) {
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
        }
    }

    private void renderPointGlow(MatrixStack matrices, float x, float y, float size, float show, int colorIndex) {
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Color color = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(colorIndex), (int) (210.0f * show)));
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x - size, y - size, 0.0f).texture(0.0f, 0.0f).color(color.getRGB());
        buffer.vertex(matrix, x - size, y + size, 0.0f).texture(0.0f, 1.0f).color(color.getRGB());
        buffer.vertex(matrix, x + size, y + size, 0.0f).texture(1.0f, 1.0f).color(color.getRGB());
        buffer.vertex(matrix, x + size, y - size, 0.0f).texture(1.0f, 0.0f).color(color.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderSparks(MatrixStack matrices, float show) {
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Spark spark : sparks) {
            float alpha = spark.life * show;
            if (alpha <= 0.01f) {
                continue;
            }

            Color color = blendImpactColor(ColorUtil.setAlpha(UIColors.gradient(spark.colorIndex), (int) (220.0f * alpha)));
            float size = spark.size * spark.life;
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, spark.x - size, spark.y - size, 0.0f).texture(0.0f, 0.0f).color(color.getRGB());
            buffer.vertex(matrix, spark.x - size, spark.y + size, 0.0f).texture(0.0f, 1.0f).color(color.getRGB());
            buffer.vertex(matrix, spark.x + size, spark.y + size, 0.0f).texture(1.0f, 1.0f).color(color.getRGB());
            buffer.vertex(matrix, spark.x + size, spark.y - size, 0.0f).texture(1.0f, 0.0f).color(color.getRGB());
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private static float[][] starVertices(float radius, float angleOffset) {
        float innerRadius = radius * 0.46f;
        float[][] points = new float[10][2];

        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * i / 5.0 + angleOffset;
            float currentRadius = i % 2 == 0 ? radius : innerRadius;
            points[i][0] = (float) Math.cos(angle) * currentRadius;
            points[i][1] = (float) Math.sin(angle) * currentRadius;
        }

        return points;
    }

    private static float cos(float radians) {
        return (float) Math.cos(radians);
    }

    private static float sin(float radians) {
        return (float) Math.sin(radians);
    }

    private Color blendImpactColor(Color color) {
        if (impactProgress <= 0.0f) {
            return color;
        }

        return ColorUtil.interpolate(new Color(255, 80, 92, color.getAlpha()), color, Math.min(1.0f, impactProgress));
    }

    @Override
    public void updateTarget() {
        super.updateTarget();
        if (currentTarget == null) {
            impactProgress = 0.0f;
            pulseOffset = 0.0f;
            prevHurtTime = 0;
            glitchX = 0.0f;
            glitchZ = 0.0f;
            glitchDecay = 0.0f;
            sparks.clear();
            for (Deque<Float> trail : vertexTrails) {
                trail.clear();
            }
        }
    }
}

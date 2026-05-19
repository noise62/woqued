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

public class TargetEspSwords extends TargetEspMode {
    private static final int SWORD_COUNT = 6;
    private static final float SWORD_LENGTH = 1.2f;
    private static final float SWORD_WIDTH = 0.15f;
    
    private float rotation;
    private float prevRotation;
    private float bobPhase;
    private float prevBobPhase;

    @Override
    public void onUpdate() {
        if (currentTarget == null || !canDraw()) {
            return;
        }

        TargetEspModule module = TargetEspModule.getInstance();
        prevRotation = rotation;
        prevBobPhase = bobPhase;

        rotation += module.circleSpeed.getValue() * 1.2f;
        bobPhase += 0.05f;
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) {
            return;
        }

        MatrixStack matrices = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        TargetEspModule module = TargetEspModule.getInstance();

        double baseX = getTargetX() - camera.getPos().x;
        double baseY = getTargetY() - camera.getPos().y + currentTarget.getHeight() / 2.0;
        double baseZ = getTargetZ() - camera.getPos().z;

        float show = (float) showAnimation.getValue();
        float sizeAnim = MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue());
        float radius = (currentTarget.getWidth() * 0.8f + module.circleSize.getValue() * 0.2f) * sizeAnim;
        float rot = MathUtil.interpolate(prevRotation, rotation);
        float bob = MathUtil.interpolate(prevBobPhase, bobPhase);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.polygonOffset(-1.0f, -10.0f);
        RenderSystem.enablePolygonOffset();

        try {
            matrices.push();
            matrices.translate(baseX, baseY, baseZ);

            for (int i = 0; i < SWORD_COUNT; i++) {
                float angleOffset = (360f / SWORD_COUNT) * i;
                float currentAngle = rot + angleOffset;
                float bobOffset = (float) Math.sin(bob + i * 0.5f) * 0.15f;

                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(currentAngle));
                matrices.translate(radius, bobOffset, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45f));

                renderSword(matrices, show, i);
                matrices.pop();
            }

            matrices.pop();
        } finally {
            RenderSystem.disablePolygonOffset();
            RenderSystem.polygonOffset(0.0f, 0.0f);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private void renderSword(MatrixStack matrices, float show, int index) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        Color bladeColor = ColorUtil.setAlpha(UIColors.gradient(index * 60), (int) (200 * show));
        Color bladeDark = ColorUtil.setAlpha(UIColors.gradient(index * 60).darker(), (int) (180 * show));
        Color bladeSide = ColorUtil.setAlpha(UIColors.gradient(index * 60).darker().darker(), (int) (160 * show));
        Color tipColor = ColorUtil.setAlpha(Color.WHITE, (int) (255 * show));
        
        float thickness = SWORD_WIDTH * 0.25f;
        
        // Render glow
        if (TargetEspModule.getInstance().circleBloom.getValue()) {
            RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            
            Color glowColor = ColorUtil.setAlpha(UIColors.gradient(index * 60), (int) (80 * show));
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            
            buffer.vertex(matrix, -SWORD_WIDTH, 0, 0).texture(0f, 0f).color(glowColor.getRGB());
            buffer.vertex(matrix, -SWORD_WIDTH, SWORD_LENGTH * 1.3f, 0).texture(0f, 1f).color(glowColor.getRGB());
            buffer.vertex(matrix, SWORD_WIDTH, SWORD_LENGTH * 1.3f, 0).texture(1f, 1f).color(glowColor.getRGB());
            buffer.vertex(matrix, SWORD_WIDTH, 0, 0).texture(1f, 0f).color(glowColor.getRGB());
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        // Render 3D sword
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        // === BLADE ===
        float bladeStart = 0.2f;
        float bladeEnd = SWORD_LENGTH - 0.2f;
        
        // Blade front
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeStart, thickness / 2).color(bladeColor.getRGB());
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeEnd, thickness / 2).color(bladeColor.getRGB());
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeEnd, thickness / 2).color(bladeColor.getRGB());
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeStart, thickness / 2).color(bladeColor.getRGB());
        
        // Blade back
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeStart, -thickness / 2).color(bladeDark.getRGB());
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeEnd, -thickness / 2).color(bladeDark.getRGB());
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeEnd, -thickness / 2).color(bladeDark.getRGB());
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeStart, -thickness / 2).color(bladeDark.getRGB());
        
        // Blade left
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeStart, -thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeEnd, -thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeEnd, thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, -SWORD_WIDTH / 2, bladeStart, thickness / 2).color(bladeSide.getRGB());
        
        // Blade right
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeStart, thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeEnd, thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeEnd, -thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, SWORD_WIDTH / 2, bladeStart, -thickness / 2).color(bladeSide.getRGB());
        
        // === TIP ===
        float tipWidth = SWORD_WIDTH / 3;
        float tipEnd = SWORD_LENGTH;
        
        // Tip front
        buffer.vertex(matrix, -tipWidth, bladeEnd, thickness / 2).color(bladeColor.getRGB());
        buffer.vertex(matrix, 0, tipEnd, 0).color(tipColor.getRGB());
        buffer.vertex(matrix, tipWidth, bladeEnd, thickness / 2).color(bladeColor.getRGB());
        buffer.vertex(matrix, tipWidth, bladeEnd, thickness / 2).color(bladeColor.getRGB());
        
        // Tip back
        buffer.vertex(matrix, tipWidth, bladeEnd, -thickness / 2).color(bladeDark.getRGB());
        buffer.vertex(matrix, 0, tipEnd, 0).color(tipColor.getRGB());
        buffer.vertex(matrix, -tipWidth, bladeEnd, -thickness / 2).color(bladeDark.getRGB());
        buffer.vertex(matrix, -tipWidth, bladeEnd, -thickness / 2).color(bladeDark.getRGB());
        
        // Tip left
        buffer.vertex(matrix, -tipWidth, bladeEnd, -thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, 0, tipEnd, 0).color(tipColor.getRGB());
        buffer.vertex(matrix, -tipWidth, bladeEnd, thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, -tipWidth, bladeEnd, thickness / 2).color(bladeSide.getRGB());
        
        // Tip right
        buffer.vertex(matrix, tipWidth, bladeEnd, thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, 0, tipEnd, 0).color(tipColor.getRGB());
        buffer.vertex(matrix, tipWidth, bladeEnd, -thickness / 2).color(bladeSide.getRGB());
        buffer.vertex(matrix, tipWidth, bladeEnd, -thickness / 2).color(bladeSide.getRGB());
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // === HILT, HANDLE, POMMEL ===
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        Color hiltColor = ColorUtil.setAlpha(new Color(180, 150, 80), (int) (220 * show));
        Color hiltDark = ColorUtil.setAlpha(new Color(140, 110, 60), (int) (220 * show));
        float hiltWidth = SWORD_WIDTH * 2.5f;
        float hiltThick = thickness * 1.5f;
        float hiltStart = 0.1f;
        float hiltEnd = 0.25f;
        
        // Hilt - all 6 faces
        addBox(buffer, matrix, -hiltWidth / 2, hiltStart, -hiltThick / 2, hiltWidth / 2, hiltEnd, hiltThick / 2, hiltColor, hiltDark);
        
        Color handleColor = ColorUtil.setAlpha(new Color(100, 80, 60), (int) (220 * show));
        Color handleDark = ColorUtil.setAlpha(new Color(70, 50, 40), (int) (220 * show));
        float handleWidth = SWORD_WIDTH * 0.8f;
        float handleThick = thickness * 1.2f;
        float handleStart = -0.2f;
        float handleEnd = 0.1f;
        
        // Handle - all 6 faces
        addBox(buffer, matrix, -handleWidth / 2, handleStart, -handleThick / 2, handleWidth / 2, handleEnd, handleThick / 2, handleColor, handleDark);
        
        Color pommelColor = ColorUtil.setAlpha(new Color(200, 170, 100), (int) (220 * show));
        Color pommelDark = ColorUtil.setAlpha(new Color(160, 130, 70), (int) (220 * show));
        float pommelSize = SWORD_WIDTH * 1.2f;
        float pommelThick = thickness * 1.8f;
        float pommelStart = -0.25f;
        float pommelEnd = -0.2f;
        
        // Pommel - all 6 faces
        addBox(buffer, matrix, -pommelSize / 2, pommelStart, -pommelThick / 2, pommelSize / 2, pommelEnd, pommelThick / 2, pommelColor, pommelDark);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void addBox(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, Color light, Color dark) {
        // Front (+Z)
        buffer.vertex(matrix, x1, y1, z2).color(light.getRGB());
        buffer.vertex(matrix, x1, y2, z2).color(light.getRGB());
        buffer.vertex(matrix, x2, y2, z2).color(light.getRGB());
        buffer.vertex(matrix, x2, y1, z2).color(light.getRGB());
        
        // Back (-Z)
        buffer.vertex(matrix, x2, y1, z1).color(dark.getRGB());
        buffer.vertex(matrix, x2, y2, z1).color(dark.getRGB());
        buffer.vertex(matrix, x1, y2, z1).color(dark.getRGB());
        buffer.vertex(matrix, x1, y1, z1).color(dark.getRGB());
        
        // Left (-X)
        buffer.vertex(matrix, x1, y1, z1).color(dark.getRGB());
        buffer.vertex(matrix, x1, y2, z1).color(dark.getRGB());
        buffer.vertex(matrix, x1, y2, z2).color(light.getRGB());
        buffer.vertex(matrix, x1, y1, z2).color(light.getRGB());
        
        // Right (+X)
        buffer.vertex(matrix, x2, y1, z2).color(light.getRGB());
        buffer.vertex(matrix, x2, y2, z2).color(light.getRGB());
        buffer.vertex(matrix, x2, y2, z1).color(dark.getRGB());
        buffer.vertex(matrix, x2, y1, z1).color(dark.getRGB());
        
        // Top (+Y)
        buffer.vertex(matrix, x1, y2, z1).color(light.getRGB());
        buffer.vertex(matrix, x2, y2, z1).color(light.getRGB());
        buffer.vertex(matrix, x2, y2, z2).color(light.getRGB());
        buffer.vertex(matrix, x1, y2, z2).color(light.getRGB());
        
        // Bottom (-Y)
        buffer.vertex(matrix, x1, y1, z2).color(dark.getRGB());
        buffer.vertex(matrix, x2, y1, z2).color(dark.getRGB());
        buffer.vertex(matrix, x2, y1, z1).color(dark.getRGB());
        buffer.vertex(matrix, x1, y1, z1).color(dark.getRGB());
    }

    @Override
    public void updateTarget() {
        super.updateTarget();
        if (currentTarget == null) {
            bobPhase = 0f;
            prevBobPhase = 0f;
        }
    }
}

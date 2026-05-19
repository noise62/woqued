package worst.woqued.client.features.modules.render;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.utils.color.UIColors;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

@ModuleRegister(name = "BlockOverlay", category = Category.RENDER)
public class BlockOverlayModule extends Module {
    private static final BlockOverlayModule instance = new BlockOverlayModule();

    private final ModeSetting colorMode = new ModeSetting("Color").value("Theme").values("Theme");
    private final BooleanSetting filled = new BooleanSetting("Fill").value(true);
    private final BooleanSetting topFace = new BooleanSetting("Top Face").value(true);
    private final BooleanSetting fullBlock = new BooleanSetting("Full block").value(true);

    public BlockOverlayModule() {
        addSettings(colorMode, filled, topFace, fullBlock);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null || mc.crosshairTarget == null) {
                return;
            }

            if (!(mc.crosshairTarget instanceof BlockHitResult result) || result.getType() != HitResult.Type.BLOCK) {
                return;
            }

            BlockPos pos = result.getBlockPos();
            if (pos == null || !mc.world.isChunkLoaded(pos)) {
                return;
            }

            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) {
                return;
            }

            Color color = colorMode.is("Theme") ? UIColors.primary() : Color.WHITE;
            Vec3d camera = mc.getEntityRenderDispatcher().camera.getPos();
            MatrixStack matrixStack = event.matrixStack();

            matrixStack.push();
            matrixStack.translate(-camera.x, -camera.y, -camera.z);
            setupRender();

            try {
                for (Box box : resolveBoxes(state, pos)) {
                    if (!isValidBox(box)) {
                        continue;
                    }

                    if (filled.getValue()) {
                        drawFill(matrixStack, box, new Color(color.getRed(), color.getGreen(), color.getBlue(), 46));
                    }

                    if (topFace.getValue()) {
                        drawTopFace(matrixStack, box, new Color(color.getRed(), color.getGreen(), color.getBlue(), 170));
                    }

                    drawOutline(matrixStack, box, new Color(color.getRed(), color.getGreen(), color.getBlue(), 235), 1.65f);
                }
            } finally {
                cleanupRender();
                matrixStack.pop();
            }
        }));

        addEvents(renderEvent);
    }

    private void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private void cleanupRender() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void drawFill(MatrixStack matrices, Box box, Color color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        quad(buffer, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
        quad(buffer, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        quad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        quad(buffer, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawTopFace(MatrixStack matrices, Box box, Color color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y = (float) box.maxY + 0.003f;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float z2 = (float) box.maxZ;

        quad(buffer, matrix, x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawOutline(MatrixStack matrices, Box box, Color color, float width) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(width);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        line(buffer, matrix, x1, y1, z1, x2, y1, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y1, z2, color);
        line(buffer, matrix, x2, y1, z2, x1, y1, z2, color);
        line(buffer, matrix, x1, y1, z2, x1, y1, z1, color);
        line(buffer, matrix, x1, y2, z1, x2, y2, z1, color);
        line(buffer, matrix, x2, y2, z1, x2, y2, z2, color);
        line(buffer, matrix, x2, y2, z2, x1, y2, z2, color);
        line(buffer, matrix, x1, y2, z2, x1, y2, z1, color);
        line(buffer, matrix, x1, y1, z1, x1, y2, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y2, z1, color);
        line(buffer, matrix, x2, y1, z2, x2, y2, z2, color);
        line(buffer, matrix, x1, y1, z2, x1, y2, z2, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1f);
    }

    private void quad(BufferBuilder buffer, Matrix4f matrix,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4,
                      Color color) {
        vertex(buffer, matrix, x1, y1, z1, color);
        vertex(buffer, matrix, x2, y2, z2, color);
        vertex(buffer, matrix, x3, y3, z3, color);
        vertex(buffer, matrix, x4, y4, z4, color);
    }

    private void line(BufferBuilder buffer, Matrix4f matrix,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      Color color) {
        buffer.vertex(matrix, x1, y1, z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.vertex(matrix, x2, y2, z2).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Color color) {
        buffer.vertex(matrix, x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private boolean isValidBox(Box box) {
        return Double.isFinite(box.minX)
                && Double.isFinite(box.minY)
                && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX)
                && Double.isFinite(box.maxY)
                && Double.isFinite(box.maxZ)
                && box.maxX > box.minX
                && box.maxY > box.minY
                && box.maxZ > box.minZ;
    }

    private java.util.List<Box> resolveBoxes(BlockState state, BlockPos pos) {
        if (fullBlock.getValue()) {
            return java.util.List.of(new Box(pos).expand(0.002));
        }

        VoxelShape shape = state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player));
        if (shape.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<Box> boxes = new java.util.ArrayList<>();
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> boxes.add(new Box(
                pos.getX() + minX,
                pos.getY() + minY,
                pos.getZ() + minZ,
                pos.getX() + maxX,
                pos.getY() + maxY,
                pos.getZ() + maxZ
        ).expand(0.002)));
        return boxes;
    }

    public static BlockOverlayModule getInstance() {
        return instance;
    }
}

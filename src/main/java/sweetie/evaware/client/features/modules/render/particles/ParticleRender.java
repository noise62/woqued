package sweetie.evaware.client.features.modules.render.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

@Setter
@Getter
@Accessors(fluent = true, chain = true)
public class ParticleRender implements QuickImports {
    private float prevX, prevY, prevZ;
    private float x, y, z;
    private float motionX, motionY, motionZ;
    private int lifeTime;
    private int maxLife;
    private float prevSize = 0f;
    private float rotation, prevRotation = 0f;
    private float rotateSpeed = 20f, size;
    private int index;
    private Identifier identifier;
    private boolean dropPhysics, rotating;

    private float spawnDuration, dyingDuration;

    private final TimerUtil timerUtil = new TimerUtil();
    private final AnimationUtil alphaAnimation = new AnimationUtil();
    private boolean gravityFalls = false;

    private boolean trail;
    private float trailLength = 5f;
    private boolean dyingEffect;
    private final Deque<Vec3d> trailPoints = new ArrayDeque<>();

    public ParticleRender(float x, float y, float z, int lifeTime) {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxLife = MathUtil.randomInRange(Math.max(lifeTime / 2, 0), lifeTime);
        this.rotation = MathUtil.randomInRange(-180f, 180f);
    }

    public static String[] textures = new String[]{
            "Spark", "Star", "Heart", "Dollar", "Snowflake", "Glow", "Ball",
    };

    public static Identifier getTexture(String mode) {
        return switch (mode) {
            case "Spark" -> FileUtil.getImage("particles/spark_" + MathUtil.randomInRange(1, 4));
            default -> FileUtil.getImage("particles/" + mode.toLowerCase());
        };
    }

    public boolean update() {
        float gravity = gravityFalls ? (float) alphaAnimation.getValue() * 0.3f : 1f;

        prevX = x;
        prevY = y;
        prevZ = z;

        x += motionX;
        y += motionY * gravity;
        z += motionZ;

        double speed = Math.sqrt((motionX * motionX + motionZ * motionZ));
        float halfSize = prevSize;

        if (posBlock(x, y - halfSize - 0.05f, z)) {
            motionY = -motionY / 1.1f;
            motionX /= 1.1f;
            motionZ /= 1.1f;
        } else {
            if (posBlock(x - (float) speed - halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x - (float) speed - halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z) ||
                    posBlock(x - (float) speed - halfSize, y, z) ||
                    posBlock(x, y, z + (float) speed + halfSize) ||
                    posBlock(x, y, z - (float) speed - halfSize)) {
                motionX = -motionX;
                motionZ = -motionZ;
                maxLife--;
            } else if (dropPhysics) {
                motionY -= 0.02f;
            }
        }

        prevRotation = rotation;
        rotation -= (prevRotation > 0) ? -rotateSpeed : rotateSpeed;

        if (!gravityFalls) {
            float scale = 1.1f;
            motionX /= scale;
            motionY /= scale;
            motionZ /= scale;
        }

        if (trail) {
            trailPoints.addFirst(new Vec3d(x, y, z));
            while (trailPoints.size() > trailLength) trailPoints.removeLast();
        }

        return mc.player.getPos().distanceTo(new Vec3d(x, y, z)) >= 80 ||
                alphaAnimation.getValue() <= 0.0 && timerUtil.finished((spawnDuration + dyingDuration + maxLife) * 50);
    }

    private float alphaPC() {
        return (float) alphaAnimation.getValue();
    }

    private int alpha() {
        return (int) (255 * alphaPC());
    }

    public void updateAlpha() {
        alphaAnimation.update();

        float alphaAnim = alphaPC();

        if (alphaAnim <= 0.0 && !timerUtil.finished(spawnDuration * 50))
            alphaAnimation.run(1.0, (long) (spawnDuration * 50), Easing.QUINT_OUT);

        if (alphaAnim >= 1.0 && timerUtil.finished((spawnDuration + maxLife) * 50))
            alphaAnimation.run(0.0, (long) (dyingDuration * 50), Easing.QUINT_OUT);
    }

    public void render(MatrixStack matrixStack) {
        if (!PlayerUtil.canSee(new Vec3d(x, y, z))) return;

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Camera camera = mc.gameRenderer.getCamera();
        Color primaryColor = ColorUtil.setAlpha(UIColors.gradient(index * 90), alpha());
        Vec3d interpolatedPos = interpolatePosition(prevX, prevY, prevZ, x, y, z);

        float halfSize = MathUtil.interpolate(prevSize, (size * alphaPC()));
        prevSize = halfSize;

        RenderSystem.setShaderTexture(0, identifier);
        matrixStack.translate(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        if (rotating) matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathUtil.interpolate(prevRotation, rotation)));

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(matrix4f, halfSize, -halfSize, 0f).texture(0f, 1f).color(primaryColor.getRGB());
        bufferBuilder.vertex(matrix4f, -halfSize, -halfSize, 0f).texture(1f, 1f).color(primaryColor.getRGB());
        bufferBuilder.vertex(matrix4f, -halfSize, halfSize, 0f).texture(1f, 0f).color(primaryColor.getRGB());
        bufferBuilder.vertex(matrix4f, halfSize, halfSize, 0f).texture(0f, 0f).color(primaryColor.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public void renderTrail(MatrixStack matrixStack) {
        if (!trail || trailPoints.size() <= 1) return;
        if (!PlayerUtil.canSee(new Vec3d(x, y, z))) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5f);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f mat = matrixStack.peek().getPositionMatrix();
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        Color col = ColorUtil.setAlpha(UIColors.gradient(index * 90), alpha());

        double interpX = MathUtil.interpolate(prevX, x);
        double interpY = MathUtil.interpolate(prevY, y);
        double interpZ = MathUtil.interpolate(prevZ, z);

        Vec3d last = null;
        for (Vec3d p : trailPoints) {
            double smoothX = MathUtil.interpolate(p.x, interpX, 0.05);
            double smoothY = MathUtil.interpolate(p.y, interpY, 0.05);
            double smoothZ = MathUtil.interpolate(p.z, interpZ, 0.05);
            Vec3d smooth = new Vec3d(smoothX, smoothY, smoothZ);

            if (last != null) {
                buf.vertex(mat, (float)(last.x - cam.x), (float)(last.y - cam.y), (float)(last.z - cam.z))
                        .color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
                buf.vertex(mat, (float)(smooth.x - cam.x), (float)(smooth.y - cam.y), (float)(smooth.z - cam.z))
                        .color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
            }
            last = smooth;
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private boolean posBlock(float x, float y, float z) {
        Block block = mc.world != null ? mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() : null;
        return block != null &&
                !(block instanceof AirBlock) &&
                block != Blocks.WATER &&
                block != Blocks.LAVA &&
                block != Blocks.SEAGRASS &&
                block != Blocks.TALL_SEAGRASS &&
                block != Blocks.SHORT_GRASS &&
                block != Blocks.TALL_GRASS &&
                block != Blocks.FERN &&
                block != Blocks.DEAD_BUSH &&
                block != Blocks.VINE &&
                block != Blocks.SNOW &&
                block != Blocks.POPPY &&
                block != Blocks.DANDELION &&
                block != Blocks.BROWN_MUSHROOM &&
                block != Blocks.RED_MUSHROOM;
    }

    private Vec3d interpolatePosition(float prevX, float prevY, float prevZ, float currentX, float currentY, float currentZ) {
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        double interpolatedX = MathUtil.interpolate(prevX, currentX) - cameraX;
        double interpolatedY = MathUtil.interpolate(prevY, currentY) - cameraY;
        double interpolatedZ = MathUtil.interpolate(prevZ, currentZ) - cameraZ;

        return new Vec3d(interpolatedX, interpolatedY, interpolatedZ);
    }
}

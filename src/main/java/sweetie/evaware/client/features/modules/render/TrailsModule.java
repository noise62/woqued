package sweetie.evaware.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.*;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.system.files.FileUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Trails", category = Category.RENDER)
public class TrailsModule extends Module {
    @Getter private static final TrailsModule instance = new TrailsModule();

    private final SliderSetting length = new SliderSetting("Length").value(1500f).range(500f, 3000f).step(100f);
    private final SliderSetting size = new SliderSetting("Size").value(0.2f).range(0.05f, 0.3f).step(0.01f);
    private final BooleanSetting renderInFirstPerson = new BooleanSetting("In first person").value(false);
    private final BooleanSetting physics = new BooleanSetting("Physics").value(true);
    private final SliderSetting fadeTime = new SliderSetting("Fade Time").value(250f).range(100f, 1000f).step(50f);

    private final List<TrailParticle> particles = new ArrayList<>();
    private final Identifier bloomTexture = FileUtil.getImage("particles/glow");

    // ета кагуне как у канеки курва
    public TrailsModule() {
        addSettings(length, size, renderInFirstPerson, physics, fadeTime);
    }

    @Override
    public void onEnable() {
        particles.clear();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            particles.removeIf(particle -> particle.shouldRemove(length.getValue().intValue()));

            boolean isFirstPerson = mc.options.getPerspective() == Perspective.FIRST_PERSON;

            if (isFirstPerson && !renderInFirstPerson.getValue()) {
                return;
            }

            Vec3d playerPos = mc.player.getPos();
            particles.add(new TrailParticle(
                    new Vec3d(playerPos.x, playerPos.y + mc.player.getHeight() * (isFirstPerson ? 0.2 : 0.5), playerPos.z),
                    particles.size()
            ));
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            MatrixStack matrixStack = event.matrixStack();

            drawTrail(matrixStack, physics.getValue(), size.getValue(), fadeTime.getValue().intValue(), length.getValue().intValue(), particles);
        }));

        addEvents(updateEvent, renderEvent);
    }

    private void drawTrail(MatrixStack matrixStack,
                          boolean physics, float size, int fadeTime, int length,
                          List<TrailParticle> particles
    ) {
        int index = 0;

        for (TrailParticle particle : particles) {
            particle.update(physics);
            if (index > 0) {
                TrailParticle prevParticle = particles.get(index - 1);
                Vec3d prevPos = prevParticle.getPosition();
                Vec3d currentPos = particle.getPosition();

                float smoothFactor = 0.2f;
                Vec3d smoothedPos = new Vec3d(
                        MathUtil.interpolate(prevPos.x, currentPos.x, smoothFactor),
                        MathUtil.interpolate(prevPos.y, currentPos.y, smoothFactor),
                        MathUtil.interpolate(prevPos.z, currentPos.z, smoothFactor)
                );
                prevParticle.setPosition(smoothedPos);
            }

            RenderUtil.WORLD.startRender(matrixStack);
            renderParticle(matrixStack, particle, size, fadeTime, length, particles);
            RenderUtil.WORLD.endRender(matrixStack);

            index++;
        }
    }

    private void renderParticle(MatrixStack matrixStack, TrailParticle particle, float size, int fadeTime, int length, List<TrailParticle> particles) {
        particle.handleAlphaTransitions(fadeTime, length);
        Color color = ColorUtil.setAlpha(UIColors.gradient(particle.getIndex() * 30), (int) particle.getAlpha());

        Vec3d pos = particle.getPosition();

        float bloomSize = size;
        if (particles.indexOf(particle) > 0) {
            TrailParticle prev = particles.get(particles.indexOf(particle) - 1);
            double distance = pos.distanceTo(prev.getPosition());
            bloomSize = (float) Math.max(size, Math.min(size * 3.3, distance * 4));
        }

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        Camera gameRendererCamera = mc.gameRenderer.getCamera();
        Vec3d renderCamera = mc.getEntityRenderDispatcher().camera.getPos();

        RenderSystem.setShaderTexture(0, bloomTexture);
        matrixStack.translate(pos.x - renderCamera.x, pos.y - renderCamera.y, pos.z - renderCamera.z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-gameRendererCamera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(gameRendererCamera.getPitch()));

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(matrix, bloomSize, -bloomSize, 0f).texture(0f, 1f).color(color.getRGB());
        bufferBuilder.vertex(matrix, -bloomSize, -bloomSize, 0f).texture(1f, 1f).color(color.getRGB());
        bufferBuilder.vertex(matrix, -bloomSize, bloomSize, 0f).texture(1f, 0f).color(color.getRGB());
        bufferBuilder.vertex(matrix, bloomSize, bloomSize, 0f).texture(0f, 0f).color(color.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    @Getter
    @Setter
    public static class TrailParticle {
        private Vec3d position;
        private Vec3d velocity;
        private final int index;
        private final TimerUtil timer = new TimerUtil();
        private final AnimationUtil alphaAnimation = new AnimationUtil();
        private float alpha = 255f;

        public TrailParticle(Vec3d position, int index) {
            this.position = position;
            this.velocity = new Vec3d(
                    MathUtil.randomInRange(-0.01, 0.01),
                    MathUtil.randomInRange(-0.01, 0.01),
                    MathUtil.randomInRange(-0.01, 0.01)
            );
            this.index = index;
        }

        public void handleAlphaTransitions(int fadeTime, int maxLife) {
            alphaAnimation.update();
            float currentAlpha = (float) alphaAnimation.getValue();

            if (currentAlpha <= 0.0 && !timer.finished(fadeTime)) {
                alphaAnimation.run(255.0, fadeTime, Easing.LINEAR);
            }

            if (currentAlpha >= 255.0 && timer.finished(maxLife - fadeTime)) {
                alphaAnimation.run(0.0, fadeTime, Easing.LINEAR);
            }

            alpha = (float) alphaAnimation.getValue();
        }

        public boolean shouldRemove(int maxLife) {
            double distance = position.distanceTo(mc.player.getPos());
            boolean expired = timer.finished(maxLife) && alpha <= 0.0;

            return distance >= 80 || expired;
        }

        public void update(boolean enablePhysics) {
            if (enablePhysics) {
                applyPhysics();
            } else {
                updateWithoutPhysics();
            }
        }

        private void applyPhysics() {
            if (isSolidBlock(position.x, position.y, position.z + velocity.z)) {
                velocity = new Vec3d(velocity.x, velocity.y, -velocity.z * 0.8);
            }

            if (isSolidBlock(position.x, position.y + velocity.y, position.z)) {
                velocity = new Vec3d(velocity.x * 0.999, -velocity.y * 0.7, velocity.z * 0.999);
            }

            if (isSolidBlock(position.x + velocity.x, position.y, position.z)) {
                velocity = new Vec3d(-velocity.x * 0.8, velocity.y, velocity.z);
            }

            updateWithoutPhysics();
        }

        private void updateWithoutPhysics() {
            position = position.add(velocity);
            velocity = velocity.multiply(0.999);
        }

        private boolean isSolidBlock(double x, double y, double z) {
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            BlockState state = mc.world.getBlockState(pos);
            Block block = state.getBlock();
            return isValidBlock(block);
        }

        private boolean isValidBlock(Block block) {
            return !(block instanceof AirBlock)
                    && !(block instanceof ButtonBlock)
                    && !(block instanceof TorchBlock)
                    && !(block instanceof LeverBlock)
                    && !(block instanceof AbstractPressurePlateBlock)
                    && !(block instanceof CarpetBlock)
                    && !(block instanceof FluidBlock);
        }
    }
}
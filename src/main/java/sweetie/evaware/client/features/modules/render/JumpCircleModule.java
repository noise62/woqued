package sweetie.evaware.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.move.JumpEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Jump Circle", category = Category.RENDER)
public class JumpCircleModule extends Module {
    @Getter private static final JumpCircleModule instance = new JumpCircleModule();

    private final ModeSetting texture = new ModeSetting("Texture").value("Glow").values("Lean", "Glow");
    private final ModeSetting outAnimation = new ModeSetting("Out animation").value("None").values("In", "None");
    private final SliderSetting size = new SliderSetting("Size").value(2f).range(0.1f, 3f).step(0.1f);
    private final SliderSetting lifeTime = new SliderSetting("Life time").value(10f).range(1f, 30f).step(1f);
    private final SliderSetting spawnDur = new SliderSetting("Spawn duration").value(6f).range(1f, 30f).step(1f);
    private final SliderSetting dyingDur = new SliderSetting("Dying duration").value(4f).range(1f, 30f).step(1f);

    private final List<Circle> circles = new ArrayList<>();

    public JumpCircleModule() {
        addSettings(texture, outAnimation, size, lifeTime, spawnDur, dyingDur);
    }

    private String texture() {
        return "circle/" + (texture.is("Lean") ? "lean" : "glow_fat");
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            circles.removeIf(Circle::update);
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            MatrixStack matrixStack = event.matrixStack();
            RenderUtil.WORLD.startRender(matrixStack);
            RenderSystem.setShaderTexture(0, FileUtil.getImage(texture()));

            for (Circle circle : circles) {
                circle.render(matrixStack);
            }

            RenderUtil.WORLD.endRender(matrixStack);
        }));

        EventListener jumpEvent = JumpEvent.getInstance().subscribe(new Listener<>(event -> {
            circles.add(new Circle(
                    mc.player.getPos().add(0, 0.150, 0),
                    size.getValue(),
                    circles.size() * 9,
                    lifeTime.getValue().intValue() * 50,
                    spawnDur.getValue().intValue() * 50,
                    dyingDur.getValue().intValue() * 50,
                    outAnimation.getValue()
            ));
        }));

        addEvents(updateEvent, renderEvent, jumpEvent);
    }

    @RequiredArgsConstructor
    private static class Circle {
        private final Vec3d position;
        private final float size;
        private final int index;
        private final int lifeTime;
        private final int spawnDur;
        private final int dyingDur;
        private final String animMode;

        private final TimerUtil timerUtil = new TimerUtil();
        private final AnimationUtil animation = new AnimationUtil();
        private final AnimationUtil sizeAnimation = new AnimationUtil();
        private boolean isBack = false;

        public boolean update() {
            if (timerUtil.finished((spawnDur + lifeTime))) {
                isBack = true;
            }

            return animation.getValue() <= 0.1 && isBack;
        }

        public void render(MatrixStack matrixStack) {
            animation.update();
            sizeAnimation.update();

            sizeAnimation.run(
                    isBack ? (animMode.contains("None") ? 1.0 : 0.0) : 1.0,
                    isBack ? dyingDur : spawnDur,
                    Easing.SINE_OUT
            );
            animation.run(
                    isBack ? 0.0 : 1.0,
                    isBack ? dyingDur : spawnDur,
                    Easing.SINE_OUT
            );

            float anim = (float) animation.getValue();
            int alpha = (int) (anim * 255);
            float scale = (float) (sizeAnimation.getValue() * size);

            Color color1 = ColorUtil.setAlpha(UIColors.gradient(index), alpha);
            Color color2 = ColorUtil.setAlpha(UIColors.gradient(index + 90), alpha);
            Color color3 = ColorUtil.setAlpha(UIColors.gradient(index + 180), alpha);
            Color color4 = ColorUtil.setAlpha(UIColors.gradient(index + 240), alpha);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            matrixStack.push();
            matrixStack.translate(
                    position.x - mc.getEntityRenderDispatcher().camera.getPos().getX(),
                    position.y - mc.getEntityRenderDispatcher().camera.getPos().getY(),
                    position.z - mc.getEntityRenderDispatcher().camera.getPos().getZ()
            );
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(timerUtil.getElapsedTime()));
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();

            buffer.vertex(matrix, scale, -scale, 0).texture(0, 1f).color(color1.getRGB());
            buffer.vertex(matrix, -scale, -scale, 0).texture(1f, 1f).color(color4.getRGB());
            buffer.vertex(matrix, -scale, scale, 0).texture(1f, 0).color(color3.getRGB());
            buffer.vertex(matrix, scale, scale, 0).texture(0, 0).color(color2.getRGB());

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrixStack.pop();
        }
    }
}

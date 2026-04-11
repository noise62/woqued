package sweetie.evaware.client.features.modules.render.particles;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class WorldParticles extends ParticlesModule.BaseSettings {
    private final SliderSetting distance = new SliderSetting(prefix + "Distance").value(15f).range(5f, 50f).step(1f);
    private final SliderSetting height = new SliderSetting(prefix + "Height").value(8f).range(5f, 15f).step(1f);
    private final SliderSetting gravity = new SliderSetting(prefix + "Gravity").value(0.3f).range(0.1f, 1f).step(0.1f);

    private final List<Particle> particles = new ArrayList<>();
    private final TimerUtil timerUtil = new TimerUtil();

    public WorldParticles() {
        super("World");
        addSettings(distance, height, gravity);
    }

    public void toggle() {
        particles.clear();
        timerUtil.reset();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            particles.removeIf(Particle::update);

            int diff = count().getValue().intValue() - particles.size();
            if (particles.size() <= count().getValue()) {
                float d = distance.getValue();
                for (int i = 0; i < diff; i++) {
                    particles.add(new Particle(
                            (float) (mc.player.getX() + MathUtil.randomInRange(-d, d)),
                            (float) (mc.player.getY() + height.getValue()),
                            (float) (mc.player.getZ() + MathUtil.randomInRange(-d, d)),
                            0f, -MathUtil.randomInRange(gravity.getValue() * 0.1f, gravity.getValue()), 0f,
                            particles.size(),
                            ParticleRender.getTexture(textureMode().getValue()),
                            size().getValue(),
                            rotate().getValue(),
                            lifeTime().getValue().intValue(),
                            spawnDuration().getValue(), dyingDuration().getValue(),
                            trail().getValue(), trailLength().getValue().intValue()
                    ));
                }
            }
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            MatrixStack matrixStack = event.matrixStack();

            for (Particle particle : particles) {
                particle.updateAlpha();

                RenderUtil.WORLD.startRender(matrixStack);
                particle.renderTrail(matrixStack);
                RenderUtil.WORLD.endRender(matrixStack);

                RenderUtil.WORLD.startRender(matrixStack);
                particle.render(matrixStack);
                RenderUtil.WORLD.endRender(matrixStack);
            }
        }));

        addEvents(renderEvent, updateEvent);
    }

    private class Particle extends ParticleRender {
        public Particle(float x, float y, float z,
                        float motionX, float motionY, float motionZ,
                        int index, Identifier identifier,
                        float size, boolean rotate, int lifetime, float sp, float dy, boolean trail, int trailLength) {
            super(x, y, z, lifetime);
            super.gravityFalls(true).dropPhysics(true).size(size).index(index)
                    .motionX(motionX).motionY(motionY).motionZ(motionZ)
                    .identifier(identifier).rotating(rotate)
                    .spawnDuration(sp).dyingDuration(dy)
                    .trail(trail).trailLength(trailLength);
        }
    }
}
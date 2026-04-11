package sweetie.evaware.client.features.modules.render.particles;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.world.AttackEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class HitParticles extends ParticlesModule.BaseSettings {
    private final ModeSetting physics = new ModeSetting(prefix + "Physics").value("Drop").values("Fly", "Drop");
    private final SliderSetting motion = new SliderSetting(prefix + "Motion").value(0.3f).range(0.1f, 1f).step(0.1f);

    private final List<Particle> particles = new ArrayList<>();

    public HitParticles() {
        super("Hit");
        addSettings(physics, motion);
    }

    public void toggle() {
        particles.clear();
        removeAllEvents();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            particles.removeIf(Particle::update);
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

        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.entity() instanceof LivingEntity entity) {
                float m = motion.getValue();
                for (int i = 0; i < count().getValue(); i++) {
                    particles.add(new Particle(
                            (float)(entity.getX() + MathUtil.randomInRange(-0.1f, 0.1f)),
                            (float)(entity.getY() + entity.getHeight() / 2 + MathUtil.randomInRange(-0.1f, 0.1f)),
                            (float)(entity.getZ() + MathUtil.randomInRange(-0.1f, 0.1f)),
                            MathUtil.randomInRange(-m, m),
                            MathUtil.randomInRange(-m, m),
                            MathUtil.randomInRange(-m, m),
                            particles.size(),
                            ParticleRender.getTexture(textureMode().getValue()),
                            size().getValue(),
                            !physics.is("Fly"),
                            rotate().getValue(),
                            lifeTime().getValue().intValue(),
                            spawnDuration().getValue(), dyingDuration().getValue(),
                            trail().getValue(), trailLength().getValue().intValue()
                    ));
                }
            }
        }));

        addEvents(renderEvent, attackEvent, updateEvent);
    }

    private class Particle extends ParticleRender {
        public Particle(float x, float y, float z,
                        float motionX, float motionY, float motionZ,
                        int index, Identifier identifier,
                        float size, boolean drop, boolean rotate, int lifetime, float sp, float dy, boolean trail, int trailLength) {
            super(x, y, z, lifetime);
            super.gravityFalls(false).dropPhysics(drop).size(size).index(index)
                    .motionX(motionX).motionY(motionY).motionZ(motionZ)
                    .identifier(identifier).rotating(rotate)
                    .spawnDuration(sp).dyingDuration(dy)
                    .trail(trail).trailLength(trailLength);
        }
    }
}
package sweetie.evaware.client.features.modules.render.particles;

import lombok.Getter;
import lombok.experimental.Accessors;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.*;
import sweetie.evaware.api.system.backend.Configurable;

@ModuleRegister(name = "Particles", category = Category.RENDER)
public class ParticlesModule extends Module {
    @Getter private static final ParticlesModule instance = new ParticlesModule();

    private final HitParticles hitParticles = new HitParticles();
    private final WorldParticles worldParticles = new WorldParticles();

    @Getter private final BooleanSetting worldBoolean = new BooleanSetting("World").value(true).onAction(() -> {
        if (!getWorldBoolean().getValue()) {
            worldParticles.toggle();
            worldParticles.removeAllEvents();
            removeEvents(worldParticles.getEventListeners());
        } else if (isEnabled()) {
            worldParticles.onEvent();
            addEvents(worldParticles.getEventListeners());
        }
    });

    @Getter private final BooleanSetting hitBoolean = new BooleanSetting("Hit").value(false).onAction(() -> {
        if (!getHitBoolean().getValue()) {
            hitParticles.toggle();
            hitParticles.removeAllEvents();
            removeEvents(hitParticles.getEventListeners());
        } else if (isEnabled()) {
            hitParticles.onEvent();
            addEvents(hitParticles.getEventListeners());
        }
    });

    private final MultiBooleanSetting spawn = new MultiBooleanSetting("Spawn").value(
            worldBoolean,
            hitBoolean
    );
    
    @Override
    public void toggle() {
        super.toggle();
        hitParticles.toggle();
        worldParticles.toggle();
    }

    public ParticlesModule() {
        addSettings(spawn);

        for (Setting<?> setting : worldParticles.getSettings()) {
            setting.setVisible(() -> spawn.isEnabled(worldBoolean.getName()));
        }
        addSettings(worldParticles.getSettings());


        for (Setting<?> setting : hitParticles.getSettings()) {
            setting.setVisible(() -> spawn.isEnabled(hitBoolean.getName()));
        }
        addSettings(hitParticles.getSettings());
    }

    @Override
    public void onEvent() {
        if (hitBoolean.getValue()) {
            hitParticles.onEvent();
            addEvents(hitParticles.getEventListeners());
        }

        if (worldBoolean.getValue()) {
            worldParticles.onEvent();
            addEvents(worldParticles.getEventListeners());
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class BaseSettings extends Configurable {
        private final ModeSetting textureMode;
        private final SliderSetting count;
        private final SliderSetting size;
        private final SliderSetting lifeTime;
        private final SliderSetting spawnDuration;
        private final SliderSetting dyingDuration;
        private final BooleanSetting rotate;
        private final BooleanSetting trail;
        private final SliderSetting trailLength;
        private final BooleanSetting dyingEffect;

        public final String prefix;

        public BaseSettings(String prefix) {
            this.prefix = prefix + ": ";

            textureMode = new ModeSetting(this.prefix + "Texture").value("Spark").values(ParticleRender.textures);
            count = new SliderSetting(this.prefix + "Count").value(25f).range(10f, 100f).step(1f);
            size = new SliderSetting(this.prefix + "Size").value(0.2f).range(0.1f, 0.4f).step(0.05f);
            lifeTime = new SliderSetting(this.prefix + "Life time").value(10f).range(2f, 100f).step(1f);
            spawnDuration = new SliderSetting(this.prefix + "Spawn duration").value(15f).range(0f, 40f).step(1f);
            dyingDuration = new SliderSetting(this.prefix + "Dying duration").value(15f).range(0f, 40f).step(1f);
            rotate = new BooleanSetting(this.prefix + "Rotate").value(true);
            trail = new BooleanSetting(this.prefix + "Trail").value(false);
            trailLength = new SliderSetting(this.prefix + "Trail length").value(5f).range(1f, 20f).step(1f);
            dyingEffect = new BooleanSetting(this.prefix + "Dying effect").value(false);


            addSettings(
                    textureMode,
                    count, size,
                    lifeTime, spawnDuration, dyingDuration,
                    rotate, trail, trailLength
            );
        }
    }
}

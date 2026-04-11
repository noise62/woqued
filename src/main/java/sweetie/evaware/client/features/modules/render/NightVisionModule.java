package sweetie.evaware.client.features.modules.render;

import lombok.Getter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

@ModuleRegister(name = "Night Vision", category = Category.RENDER)
public class NightVisionModule extends Module {
    @Getter private static final NightVisionModule instance = new NightVisionModule();

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            add();
        }));

        addEvents(tickEvent);
    }

    private void remove() {
        if (mc.player == null) return;
        mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }

    private void add() {
        if (mc.player == null) return;
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0, false, false, false));
    }
}

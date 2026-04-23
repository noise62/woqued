package worst.woqued.client.features.modules.render;

import lombok.Getter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.utils.other.SoundUtil;

@ModuleRegister(name = "Geroin", category = Category.RENDER)
public class MellModule extends Module {
    @Getter private static final MellModule instance = new MellModule();

    @Override
    public void onEnable() {
        SoundUtil.playSound(SoundUtil.MELL_EVENT);
    }

    @Override
    public void onDisable() {
        SoundUtil.stopSounds();
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
        mc.player.removeStatusEffect(StatusEffects.NAUSEA);
    }

    private void add() {
        if (mc.player == null) return;
        // Накладываем эффект тошноты (Nausea) для эффекта вращения экрана
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, -1, 0, false, false, false));
    }
}

package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.entity.Entity;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.world.AttackEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.utils.neuro.DataCollector;

@ModuleRegister(name = "Recorder", category = Category.OTHER)
public class RecorderModule extends Module {
    @Getter private static final RecorderModule instance = new RecorderModule();
    private final DataCollector dataCollector = new DataCollector();

    private Entity target;

    @Override
    public void onDisable() {
        target = null;
        dataCollector.stopCollecting();
    }

    @Override
    public void onEnable() {
        dataCollector.startCollecting();
    }

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (target != event.entity()) target = event.entity();

            if (target != null) dataCollector.onAttack(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (target != null) dataCollector.onUpdate();
        }));

        addEvents(attackEvent, updateEvent);
    }
}

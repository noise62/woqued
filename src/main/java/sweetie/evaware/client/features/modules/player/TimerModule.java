package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.system.client.TimerManager;
import sweetie.evaware.api.utils.task.TaskPriority;

@ModuleRegister(name = "Timer", category = Category.PLAYER)
public class TimerModule extends Module {
    @Getter private static final TimerModule instance = new TimerModule();

    private final SliderSetting multiplier = new SliderSetting("Multiplier").value(2f).range(0.1f, 5f).step(0.1f);

    public TimerModule() {
        addSettings(multiplier);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            TimerManager.getInstance().addTimer(multiplier.getValue(), TaskPriority.NORMAL, this, 1);
        }));

        addEvents(tickEvent);
    }
}

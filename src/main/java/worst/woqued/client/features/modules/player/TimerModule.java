package worst.woqued.client.features.modules.player;

import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.system.client.TimerManager;
import worst.woqued.api.utils.task.TaskPriority;

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

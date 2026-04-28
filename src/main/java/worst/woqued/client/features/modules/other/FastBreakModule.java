package worst.woqued.client.features.modules.other;

import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

@ModuleRegister(name = "Fast Break", category = Category.OTHER)
public class FastBreakModule extends Module {
    @Getter private static final FastBreakModule instance = new FastBreakModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            mc.interactionManager.blockBreakingCooldown = 0;
            mc.interactionManager.cancelBlockBreaking();
        }));

        addEvents(updateEvent);
    }
}

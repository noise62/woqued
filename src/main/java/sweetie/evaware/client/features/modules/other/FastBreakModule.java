package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

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

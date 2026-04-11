package sweetie.evaware.client.features.modules.movement;

import lombok.Getter;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

@ModuleRegister(name = "No Jump Delay", category = Category.MOVEMENT)
public class NoJumpDelayModule extends Module {
    @Getter private static final NoJumpDelayModule instance = new NoJumpDelayModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            mc.player.jumpingCooldown = 0;
        }));

        addEvents(updateEvent);
    }
}

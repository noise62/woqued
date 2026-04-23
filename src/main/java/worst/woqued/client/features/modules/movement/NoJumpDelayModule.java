package worst.woqued.client.features.modules.movement;

import lombok.Getter;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

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

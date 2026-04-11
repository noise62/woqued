package sweetie.evaware.client.features.modules.movement;

import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.utils.player.MoveUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;

@ModuleRegister(name = "No Web", category = Category.MOVEMENT)
public class NoWebModule extends Module {
    @Getter private static final NoWebModule instance = new NoWebModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (PlayerUtil.isInWeb()) {
                mc.player.setVelocity(0, 0, 0);

                double verticalSpeed = 0.995;
                double horizantalSpeed = 0.19175;

                if (mc.options.jumpKey.isPressed()) {
                    mc.player.getVelocity().y = verticalSpeed;
                } else if (mc.options.sneakKey.isPressed()) {
                    mc.player.getVelocity().y = -verticalSpeed;
                }

                MoveUtil.setSpeed(horizantalSpeed);
            }
        }));

        addEvents(updateEvent);
    }
}

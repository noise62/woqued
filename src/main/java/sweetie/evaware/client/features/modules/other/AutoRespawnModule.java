package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.DeathScreen;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Respawn", category = Category.OTHER)
public class AutoRespawnModule extends Module {
    @Getter private static final AutoRespawnModule instance = new AutoRespawnModule();

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.currentScreen instanceof DeathScreen) {
                if (mc.player.deathTime > 2) {
                    mc.player.requestRespawn();
                    mc.setScreen(null);
                }
            }
        }));

        addEvents(tickEvent);
    }
}

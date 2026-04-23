package worst.woqued.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.DeathScreen;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

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

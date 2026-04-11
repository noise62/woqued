package sweetie.evaware.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.KeyEvent;
import sweetie.evaware.api.event.events.other.ScreenEvent;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.system.client.GpsManager;
import sweetie.evaware.api.system.configs.ConfigSkin;
import sweetie.evaware.api.system.configs.MacroManager;
import sweetie.evaware.api.system.draggable.DraggableManager;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.other.ScreenUtil;
import sweetie.evaware.api.utils.other.SlownessManager;

public class HeartbeatService implements QuickImports {
    @Getter private static final HeartbeatService instance = new HeartbeatService();

    public void load() {
        keyEvent();
        render2dEvent();
        tickEvent();
        screenEvent();
    }

    private void screenEvent() {
        ScreenEvent.getInstance().subscribe(new Listener<>(event -> {
            ScreenUtil.drawButton(event);
        }));
    }

    private void tickEvent() {
        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            SlownessManager.tick();

            ConfigSkin.getInstance().fetchSkin();
        }));
    }

    private void render2dEvent() {
        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.currentScreen instanceof ChatScreen) {
                DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                    if (draggable.getModule().isEnabled()) {
                        draggable.onDraw();
                    }
                });
            }

            GpsManager.getInstance().update(event.context());
        }));
    }

    private void keyEvent() {
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1 || event.key() == -999 || event.key() == -1) return;

            int action = event.action();
            int key = event.key() + (event.mouse() ? -100 : 0);

            if (mc.currentScreen == null) {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    int bind = module.getBind();
                    if (bind == key && module.hasBind()) {
                        module.toggle();
                    }
                });

                MacroManager.getInstance().onKeyPressed(key);
            }
        }));
    }
}

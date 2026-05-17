package worst.woqued.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.KeyEvent;
import worst.woqued.api.event.events.other.ScreenEvent;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.api.module.ModuleManager;
import worst.woqued.api.system.client.GpsManager;
import worst.woqued.api.system.configs.BindManager;
import worst.woqued.api.system.configs.ConfigSkin;
import worst.woqued.api.system.configs.MacroManager;
import worst.woqued.api.system.draggable.DraggableManager;
import worst.woqued.api.system.interfaces.QuickImports;
import worst.woqued.api.utils.other.ScreenUtil;
import worst.woqued.api.utils.other.SlownessManager;

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

            int key = event.key() + (event.mouse() ? -100 : 0);

            if (mc.currentScreen == null) {
                boolean handled = BindManager.getInstance().getBinds().stream()
                        .anyMatch(b -> b.getKey() == key);

                if (handled) {
                    BindManager.getInstance().onKeyPressed(key);
                } else {
                    ModuleManager.getInstance().getModules().forEach(module -> {
                        int bind = module.getBind();
                        if (bind == key && module.hasBind()) {
                            module.toggle();
                        }
                    });
                }

                MacroManager.getInstance().onKeyPressed(key);
            }
        }));
    }
}

package worst.woqued.client.features.modules.other;

import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.KeyEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BindSetting;

@Getter
@ModuleRegister(name = "Auto Ec", category = Category.OTHER)
public class AutoEcModule extends Module {
    @Getter private static final AutoEcModule instance = new AutoEcModule();

    private final BindSetting key = new BindSetting("Key").value(-999);

    public AutoEcModule() {
        addSettings(key);
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isEnabled()) return;
            if (mc.player == null || mc.world == null) return;
            if (mc.currentScreen != null) return;

            int bindKey = key.getValue();
            if (bindKey == -1 || bindKey == -999) return;

            if (event.key() == bindKey && event.action() == 1) {
                execute();
            }
        }));

        addEvents(keyEvent);
    }

    private void execute() {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendChatCommand("ec");
        }
    }
}

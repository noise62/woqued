package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Ec", category = Category.OTHER)
public class AutoEcModule extends Module {
    @Getter private static final AutoEcModule instance = new AutoEcModule();

    private boolean sent = false;

    @Override
    public void onEvent() {
        // Нет подписок на события — команда отправляется один раз при включении
    }

    @Override
    public void onEnable() {
        sent = false;
        if (mc.player != null && mc.getNetworkHandler() != null && !sent) {
            mc.getNetworkHandler().sendChatCommand("ec");
            sent = true;
        }
    }

    @Override
    public void onDisable() {
        sent = false;
    }
}

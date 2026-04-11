package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import net.minecraft.item.Items;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BindSetting;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.other.SlownessManager;

@ModuleRegister(name = "Click Pearl", category = Category.PLAYER)
public class ClickPearlModule extends Module {
    @Getter private static final ClickPearlModule instance = new ClickPearlModule();

    private final BindSetting throwKey = new BindSetting("Throw key").value(-999);
    private final BooleanSetting legit = new BooleanSetting("Legit").value(false);

    private final InventoryUtil.ItemUsage itemUsage = new InventoryUtil.ItemUsage(Items.ENDER_PEARL, this);

    public ClickPearlModule() {
        addSettings(throwKey, legit);
    }

    @Override
    public void onDisable() {
        itemUsage.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handle(!SlownessManager.isEnabled());
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handle(SlownessManager.isEnabled());
        }));

        addEvents(tickEvent, updateEvent);
    }

    private void handle(boolean tick) {
        if (tick) return;

        itemUsage.handleUse(throwKey.getValue(), legit.getValue());
    }
}

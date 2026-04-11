package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.client.KeyEvent;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BindSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.other.SlownessManager;

@ModuleRegister(name = "Item Swap", category = Category.COMBAT)
public class ItemSwapModule extends Module {
    @Getter private static final ItemSwapModule instance = new ItemSwapModule();

    private final BindSetting swapKey = new BindSetting("Swap key").value(-999);
    private final ModeSetting firstItem = new ModeSetting("First item").value("Shield").values("Shield", "GApple", "Totem", "Ball");
    private final ModeSetting secondItem = new ModeSetting("Second item").value("GApple").values("Shield", "GApple", "Totem", "Ball");

    private boolean swapping = false;

    public ItemSwapModule() {
        addSettings(swapKey, firstItem, secondItem);
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.key() == swapKey.getValue() && event.action() == 1 && mc.currentScreen == null) {
                swapping = true;
            }
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!SlownessManager.isEnabled()) return;
            performSwap();
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (SlownessManager.isEnabled()) return;
            performSwap();
        }));

        addEvents(keyEvent, tickEvent, updateEvent);
    }

    private void performSwap() {
        if (!(mc.world != null && mc.player != null && mc.interactionManager != null && swapping)) return;

        Item item = getItem();

        if (item == null) {
            print("Предмет не найден");
            swapping = false;
            return;
        }

        int slot = InventoryUtil.findItem(item);
        if (slot == -1) {
            print("Предмет не найден в инвентаре");
            swapping = false;
            return;
        }

        if (SlownessManager.isEnabled()) {
            SlownessManager.applySlowness(10, () -> swap(slot));
        } else {
            swap(slot);
        }
    }

    private Item getItem() {
        Item primary = getItemByMode(firstItem.getValue());
        Item secondary = getItemByMode(secondItem.getValue());
        return mc.player.getOffHandStack().getItem() == primary ? secondary : primary;
    }

    private void swap(int slot) {
        if (mc.interactionManager == null) {
            return;
        }

        print("Свапнула на \"" + getItem().getName().getString() + "\"");

        if (SlownessManager.isEnabled()) {
            SlownessManager.applySlowness(10, () -> InventoryUtil.swapToOffhand(slot));
        } else InventoryUtil.swapToOffhand(slot);
        swapping = false;
    }

    private Item getItemByMode(String name) {
        return switch (name.toLowerCase()) {
            case "shield" -> Items.SHIELD;
            case "ball" -> Items.PLAYER_HEAD;
            case "totem" -> Items.TOTEM_OF_UNDYING;
            case "gapple" -> Items.GOLDEN_APPLE;
            default -> null;
        };
    }
}

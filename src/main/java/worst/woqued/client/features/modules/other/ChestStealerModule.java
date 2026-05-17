package worst.woqued.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.MultiBooleanSetting;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.math.TimerUtil;

@ModuleRegister(name = "Chest Stealer", category = Category.OTHER)
public class ChestStealerModule extends Module {
    @Getter private static final ChestStealerModule instance = new ChestStealerModule();

    private final TimerUtil timer = new TimerUtil();

    private final ModeSetting mode = new ModeSetting("Mode").value("FunTime").values("FunTime", "WhiteList", "Default");
    private final SliderSetting delay = new SliderSetting("Delay").value(100f).range(0f, 1000f).setVisible(() -> mode.is("WhiteList") || mode.is("Default"));

    private final MultiBooleanSetting items = new MultiBooleanSetting("Items").value(
            new BooleanSetting("Player Head").value(true),
            new BooleanSetting("Totem Of Undying").value(true),
            new BooleanSetting("Elytra").value(true),
            new BooleanSetting("Netherite Sword").value(true),
            new BooleanSetting("Netherite Helmet").value(true),
            new BooleanSetting("Netherite ChestPlate").value(true),
            new BooleanSetting("Netherite Leggings").value(true),
            new BooleanSetting("Netherite Boots").value(true),
            new BooleanSetting("Netherite Ingot").value(true),
            new BooleanSetting("Netherite Scrap").value(true)
    ).setVisible(() -> mode.is("WhiteList"));

    public ChestStealerModule() {
        addSettings(mode, delay, items);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));

        addEvents(tickEvent);
    }

    private void onTick() {
        if (mc.player == null) return;

        switch (mode.getValue()) {
            case "FunTime" -> handleFunTimeMode();
            case "WhiteList", "Default" -> handleDefaultMode();
        }
    }

    private void handleFunTimeMode() {
        if (mc.currentScreen instanceof GenericContainerScreen sh
                && sh.getTitle().getString().toLowerCase().contains("мистический")
                && !mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {

            var slots = sh.getScreenHandler().slots;
            for (int i = 0; i < slots.size(); i++) {
                var slot = slots.get(i);
                if (slot.hasStack()
                        && !slot.inventory.equals(mc.player.getInventory())
                        && timer.finished(150)) {
                    mc.interactionManager.clickSlot(sh.getScreenHandler().syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    timer.reset();
                    break;
                }
            }
        }
    }

    private void handleDefaultMode() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh)) return;

        var slots = sh.slots;
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.hasStack()
                    && !slot.inventory.equals(mc.player.getInventory())
                    && (mode.is("Default") || whiteList(slot.getStack().getItem()))
                    && timer.finished(delay.getValue())) {

                mc.interactionManager.clickSlot(sh.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                timer.reset();
                break;
            }
        }
    }

    private boolean whiteList(Item item) {
        String itemName = item.toString().toLowerCase().replace("_", "");
        return items.getList().stream()
                .anyMatch(s -> s.toLowerCase().replace(" ", "").replace("_", "").contains(itemName));
    }
}
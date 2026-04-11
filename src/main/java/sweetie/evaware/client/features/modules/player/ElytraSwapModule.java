package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import net.minecraft.item.Item;
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
import sweetie.evaware.api.system.backend.KeyStorage;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.other.SlownessManager;

@ModuleRegister(name = "Elytra Swap", category = Category.PLAYER)
public class ElytraSwapModule extends Module {
    @Getter private static final ElytraSwapModule instance = new ElytraSwapModule();

    private final BindSetting swapKey = new BindSetting("Swap key").value(-999);

    private final BindSetting launchKey = new BindSetting("Launch key").value(-999);
    private final BooleanSetting legit = new BooleanSetting("Legit").value(false).setVisible(() -> launchKey.getValue() != -999);

    private final InventoryUtil.ItemUsage itemUsage = new InventoryUtil.ItemUsage(Items.FIREWORK_ROCKET, this);
    private boolean swapUsed = false;

    public ElytraSwapModule() {
        addSettings(swapKey, launchKey, legit);
        itemUsage.setUseRotation(false);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handleMainLogic(!SlownessManager.isEnabled());
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handleMainLogic(SlownessManager.isEnabled());
        }));

        addEvents(tickEvent, updateEvent);
    }

    private void handleMainLogic(boolean slow) {
        handleFireworkLaunch(slow);
        handleChestplateSwap(slow);
    }

    public void handleFireworkLaunch(boolean tick) {
        if (tick || !mc.player.isGliding()) return;

        itemUsage.handleUse(launchKey.getValue(), legit.getValue());
    }

    public void handleChestplateSwap(boolean tick) {
        if (tick) return;

        if (KeyStorage.isPressed(swapKey.getValue())) {
            if (!swapUsed && mc.currentScreen == null) {
                if (slots() == -1) return;

                if (SlownessManager.isEnabled()) {
                    SlownessManager.applySlowness(10, () -> {
                        swapChestplate();
                        swapUsed = true;
                    });
                } else {
                    swapChestplate();
                    swapUsed = true;
                }
            }
        } else {
            swapUsed = false;
        }
    }

    public void swapChestplate() {
        if (mc.player == null || mc.interactionManager == null) return;

        if (InventoryUtil.hasElytraEquipped()) {
            int slot = slots();

            if (slot != -1) {
                if (slot >= 0 && slot <= 8) {
                    InventoryUtil.swapSlotsFull(6, slot);
                } else if (slot >= 36 && slot <= 44) {
                    int hotbarSlot = slot - 36;
                    InventoryUtil.swapSlotsFull(6, hotbarSlot);
                } else {
                    int emptySlot = InventoryUtil.findEmptySlot();
                    if (emptySlot == -1) {
                        emptySlot = InventoryUtil.findBestSlotInHotBar();
                    }

                    if (emptySlot != -1) {
                        InventoryUtil.swapSlots(slot, emptySlot);
                        InventoryUtil.swapSlotsFull(6, emptySlot);
                        InventoryUtil.swapSlots(slot, emptySlot);
                    }
                }
            }
        } else {
            int slot = slots();

            if (slot != -1) {
                if (slot >= 0 && slot <= 8) {
                    InventoryUtil.swapSlotsFull(6, slot);
                } else if (slot >= 36 && slot <= 44) {
                    int hotbarSlot = slot - 36;
                    InventoryUtil.swapSlotsFull(6, hotbarSlot);
                } else {
                    int emptySlot = InventoryUtil.findEmptySlot();
                    if (emptySlot == -1) {
                        emptySlot = InventoryUtil.findBestSlotInHotBar();
                    }

                    if (emptySlot != -1) {
                        InventoryUtil.swapSlots(slot, emptySlot);
                        InventoryUtil.swapSlotsFull(6, emptySlot);
                        InventoryUtil.swapSlots(slot, emptySlot);
                    }
                }
            }
        }
    }

    private int findBestSlotFor(Item... items) {
        for (Item item : items) {
            int slot = InventoryUtil.findItem(item);
            if (slot != -1) return slot;
        }
        return -1;
    }

    public int slots() {
        return InventoryUtil.hasElytraEquipped() ? findChestplateSlot() : findElytraSlot();
    }

    public int findElytraSlot() {
        return findBestSlotFor(Items.ELYTRA);
    }

    public int findChestplateSlot() {
        return findBestSlotFor(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE);
    }
}

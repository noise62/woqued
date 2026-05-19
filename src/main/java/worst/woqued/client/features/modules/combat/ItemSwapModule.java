package worst.woqued.client.features.modules.combat;

import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.KeyEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BindSetting;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.utils.math.TimerUtil;
import worst.woqued.api.utils.other.SlownessManager;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleRegister(name = "Item Swap", category = Category.COMBAT)
public class ItemSwapModule extends Module {
    private static final String ITEM_HEAD = "Player Head";
    private static final String ITEM_GAPPLE = "Golden Apple";
    private static final String ITEM_SHIELD = "Shield";
    private static final String ITEM_TOTEM = "Totem";

    @Getter
    private static final ItemSwapModule instance = new ItemSwapModule();

    private final ModeSetting firstItem = new ModeSetting("First item").value(ITEM_HEAD).values(ITEM_GAPPLE, ITEM_SHIELD, ITEM_HEAD, ITEM_TOTEM);
    private final ModeSetting secondItem = new ModeSetting("Second item").value(ITEM_TOTEM).values(ITEM_GAPPLE, ITEM_SHIELD, ITEM_HEAD, ITEM_TOTEM);
    private final BindSetting swapKey = new BindSetting("Bind").value(-999);
    private final BooleanSetting swapRender = new BooleanSetting("Show swap").value(true);
    private final BooleanSetting chatNotify = new BooleanSetting("Chat notify").value(false);
    private final BooleanSetting legit = new BooleanSetting("Legit").value(false);
    private final BooleanSetting onlyEnchanted = new BooleanSetting("Only enchanted totems").value(false);

    private final ConcurrentLinkedQueue<ClickTask> clickQueue = new ConcurrentLinkedQueue<>();
    private final TimerUtil actionTimer = new TimerUtil();
    private final TimerUtil keyDelay = new TimerUtil();

    private boolean useFirstItem = true;
    private boolean processing;

    private record ClickTask(int slot, int button, SlotActionType type) {
    }

    public ItemSwapModule() {
        addSettings(firstItem, secondItem, swapKey, swapRender, chatNotify, legit, onlyEnchanted);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.interactionManager == null) {
                return;
            }

            if (!clickQueue.isEmpty() && actionTimer.finished(legit.getValue() ? 95L : 50L)) {
                executeNextClick();
                actionTimer.reset();
            } else if (clickQueue.isEmpty()) {
                processing = false;
            }
        }));

        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.currentScreen != null) {
                return;
            }

            if (event.action() != 1 || event.key() != swapKey.getValue() || !keyDelay.finished(350L) || processing) {
                return;
            }

            prepareSwap();
            keyDelay.reset();
        }));

        addEvents(updateEvent, keyEvent);
    }

    private void prepareSwap() {
        String modeName = resolveTargetMode();
        Item targetItem = getItemByMode(modeName);
        if (targetItem == null || mc.player == null) {
            return;
        }

        int inventorySlot = targetItem == Items.TOTEM_OF_UNDYING
                ? findTotemSlot(onlyEnchanted.getValue(), modeName)
                : findInventorySlot(targetItem, modeName);

        if (inventorySlot == -1) {
            print("Item not found.");
            return;
        }

        Runnable swapTask = () -> {
            int serverSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
            enqueueSwap(serverSlot);
            if (chatNotify.getValue()) {
                print("Swap: " + modeName);

            }
            useFirstItem = !useFirstItem;
        };

        if ((inventorySlot >= 9 || legit.getValue()) && SlownessManager.isEnabled()) {
            SlownessManager.applySlowness(1000L, 140L, swapTask);
        } else {
            swapTask.run();
        }
    }

    private void enqueueSwap(int serverSlot) {
        clickQueue.clear();
        clickQueue.add(new ClickTask(serverSlot, 0, SlotActionType.PICKUP));
        clickQueue.add(new ClickTask(45, 0, SlotActionType.PICKUP));
        clickQueue.add(new ClickTask(serverSlot, 0, SlotActionType.PICKUP));
        processing = true;
        actionTimer.reset();
    }

    private void executeNextClick() {
        ClickTask task = clickQueue.poll();
        if (task == null || mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }

        boolean wasSprinting = mc.player.isSprinting();
        if (wasSprinting && !legit.getValue()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }

        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                task.slot,
                task.button,
                task.type,
                mc.player
        );

        if (wasSprinting && !legit.getValue()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    private String resolveTargetMode() {
        if (mc.player == null) {
            return useFirstItem ? firstItem.getValue() : secondItem.getValue();
        }

        if (matchesMode(mc.player.getOffHandStack(), firstItem.getValue())) {
            return secondItem.getValue();
        }
        if (matchesMode(mc.player.getOffHandStack(), secondItem.getValue())) {
            return firstItem.getValue();
        }

        return useFirstItem ? firstItem.getValue() : secondItem.getValue();
    }

    private boolean matchesMode(ItemStack stack, String modeName) {
        Item item = getItemByMode(modeName);
        if (item == null || stack == null || stack.isEmpty() || !stack.isOf(item)) {
            return false;
        }
        return item != Items.TOTEM_OF_UNDYING || !onlyEnchanted.getValue() || stack.hasEnchantments();
    }

    private int findInventorySlot(Item item, String modeName) {
        if (mc.player == null) {
            return -1;
        }

        if (matchesMode(mc.player.getMainHandStack(), modeName)) {
            return mc.player.getInventory().selectedSlot;
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(item) && matchesMode(stack, modeName)) {
                return i;
            }
        }
        return -1;
    }

    private int findTotemSlot(boolean enchantedOnly, String modeName) {
        if (mc.player == null) {
            return -1;
        }

        if (matchesMode(mc.player.getMainHandStack(), modeName)) {
            return mc.player.getInventory().selectedSlot;
        }

        for (int i = 35; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isOf(Items.TOTEM_OF_UNDYING)) {
                continue;
            }
            if (enchantedOnly && !stack.hasEnchantments()) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private Item getItemByMode(String name) {
        return switch (name) {
            case ITEM_HEAD -> Items.PLAYER_HEAD;
            case ITEM_GAPPLE -> Items.GOLDEN_APPLE;
            case ITEM_SHIELD -> Items.SHIELD;
            case ITEM_TOTEM -> Items.TOTEM_OF_UNDYING;
            default -> null;
        };
    }

    @Override
    public void onDisable() {
        clickQueue.clear();
        processing = false;
        useFirstItem = true;
    }
}

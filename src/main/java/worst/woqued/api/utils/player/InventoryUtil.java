package worst.woqued.api.utils.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import worst.woqued.api.module.Module;
import worst.woqued.api.system.backend.KeyStorage;
import worst.woqued.api.system.interfaces.QuickImports;
import worst.woqued.api.utils.other.NetworkUtil;
import worst.woqued.api.utils.other.SlownessManager;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationStrategy;
import worst.woqued.api.utils.rotation.rotations.SnapRotation;
import worst.woqued.api.utils.task.TaskPriority;
import worst.woqued.client.features.modules.combat.AuraModule;
import worst.woqued.client.features.modules.movement.InventoryMoveModule;

@UtilityClass
public class InventoryUtil implements QuickImports {
    public void dropSlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 1, SlotActionType.THROW, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic())
            NetworkUtil.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    public void swapToOffhand(int slot) {
        if (slot == -1) return;
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    public void swapSlots(int from, int to) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;
        int swapSlot = (to >= 0 && to <= 8) ? to : (from >= 0 && from <= 8) ? from : -1;
        if (swapSlot == -1) return;

        mc.interactionManager.clickSlot(syncId, (swapSlot == to) ? from : to, swapSlot, SlotActionType.SWAP, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic()) mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }
    public void swapSlotsFull(int from, int to) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;

        mc.interactionManager.clickSlot(syncId, from, to, SlotActionType.SWAP, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic()) mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    public void useItem(Hand hand) {
        if (mc.player == null) return;

        RotationManager rotationManager = RotationManager.getInstance();

        float currentYaw = rotationManager.getCurrentRotationPlan() != null ? rotationManager.getRotation().getYaw() : mc.player.getYaw();
        float currentPitch = rotationManager.getCurrentRotationPlan() != null ? rotationManager.getRotation().getPitch() : mc.player.getPitch();

        NetworkUtil.sendPacket(s -> new PlayerInteractItemC2SPacket(hand, s, currentYaw, currentPitch));
        mc.player.swingHand(hand);
    }

    public void swapToSlot(int slot) {
        swapToSlot(slot, true);
    }

    public void swapToSlot(int slot, boolean client) {
        if (mc.player == null) return;

        if (mc.player.getInventory().selectedSlot == slot) return;

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        if (client) mc.player.getInventory().selectedSlot = slot;
    }

    public int findItem(Item item, boolean inHotbar) {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int firstSlot = inHotbar ? 0 : 9;
        int lastSlot = inHotbar ? 9 : 36;
        int finalSlot = -1;

        for (int i = firstSlot; i < lastSlot; i++) {
            if (main.get(i).getItem() == item) {
                finalSlot = i;
            }
        }
        return finalSlot;
    }

    public int findItem(Item input) {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int slot = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == input) {
                slot = i;
                break;
            }
        }

        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    public boolean hasElytraEquipped() {
        return mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    public int findAxeInInventory(boolean inHotbar) {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int firstSlot = inHotbar ? 0 : 9;
        int lastSlot = inHotbar ? 9 : 36;

        for (int i = firstSlot; i < lastSlot; i++) {
            if (main.get(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    public int findBestSlotInHotBar() {
        int emptySlot = findEmptySlot();
        return emptySlot != -1 ? emptySlot : findNonSwordSlot();
    }

    public int findEmptySlot() {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int currentItem = mc.player.getInventory().selectedSlot;

        for (int i = 0; i < 9; i++) {
            if (main.get(i).isEmpty() && currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    private static int findNonSwordSlot() {
        if (mc.player == null) return -1;

        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int currentItem = mc.player.getInventory().selectedSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = main.get(i);
            if (!(stack.getItem() instanceof SwordItem) && currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    public int countNonEnchantedTotems() {
        if (mc.player == null) return -1;
        int count = 0;
        DefaultedList<ItemStack> main = mc.player.getInventory().main;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING) && !stack.hasEnchantments()) {
                count++;
            }
        }
        return count;
    }

    public int findBestTotemSlot(boolean saveEnchanted) {
        DefaultedList<ItemStack> main = mc.player.getInventory().main;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                if (!saveEnchanted || !stack.hasEnchantments()) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    public int findBestElytraSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ELYTRA) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public int findBestChestplateSlot() {
        if (mc.player == null) return -1;
        Item[] chestplates = {
            Items.NETHERITE_CHESTPLATE,
            Items.DIAMOND_CHESTPLATE,
            Items.IRON_CHESTPLATE,
            Items.GOLDEN_CHESTPLATE,
            Items.CHAINMAIL_CHESTPLATE,
            Items.LEATHER_CHESTPLATE
        };
        for (Item chestplate : chestplates) {
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == chestplate) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    public void swapAndUseHvH(Item item) {
        int invSlot = findItem(item, false);
        int hbSlot = findItem(item, true);

        if (mc.player.getOffHandStack().isOf(item)) {
            useItem(Hand.OFF_HAND);
            return;
        }

        if (mc.player.getMainHandStack().isOf(item)) {
            useItem(Hand.MAIN_HAND);
            return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        int bestSlot = findBestSlotInHotBar();

        if (hbSlot != -1) {
            swapToSlot(hbSlot);
            useItem(Hand.MAIN_HAND);
            swapToSlot(oldSlot);
        } else if (invSlot != -1) {
            swapSlots(invSlot, bestSlot);
            swapToSlot(bestSlot);
            useItem(Hand.MAIN_HAND);
            swapToSlot(oldSlot);
            swapSlots(invSlot, bestSlot);
        }
    }

    public void swapAndUseLegit(Item item) {
        int slot = findItem(item, true);
        if (slot == -1) {
            slot = findItem(item, false);
            if (slot == -1) return;

            int bestSlot = findBestSlotInHotBar();
            if (bestSlot == -1) return;

            swapSlots(slot, bestSlot);
            slot = bestSlot;
        }

        int previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        swapToSlot(slot);
        useItem(Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = previousSlot;
    }

    public void swapWithBypassGrim(Runnable action) {
        action.run();
    }

    public void swapWithBypassPolar(Runnable action) {
        action.run();
    }

    @Getter
    @RequiredArgsConstructor
    public static class ItemUsage {
        private final Item item;
        private final Module provider;
        
        private boolean forceUse = false;
        @Setter private boolean useRotation = true;
        private Rotation customRotation = new Rotation(Float.MIN_VALUE, Float.MIN_VALUE);

        private int targetSlot = -1;
        private int previousSlot = -1;
        private int stateDelay = 0;

        private Runnable pendingAction = null;
        private UsageState usageState = UsageState.IDLE;

        public void onDisable() {
            usageState = UsageState.IDLE;
            stateDelay = 0;
        }

        public void updateCustomRotation(Rotation rotation) {
            customRotation = rotation;
        }

        public void applyRotation() {
            if (!useRotation) return;
            float yaw = customRotation.getYaw() != Float.MIN_VALUE ? customRotation.getYaw() : mc.player.getYaw();
            float pitch = customRotation.getPitch() != Float.MIN_VALUE ? customRotation.getPitch() : mc.player.getPitch();

            RotationStrategy configurable = new RotationStrategy(new SnapRotation(), true, AuraModule.getInstance().moveCorrection.is("Free"));
            RotationManager.getInstance().addRotation(new Rotation(yaw, pitch), configurable, TaskPriority.REQUIRED, provider);
        }

        public void handleUse(boolean isLegit) {
            if (mc.currentScreen == null && pendingAction != null) {
                pendingAction.run();
            }

            if (mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) return;

            if (isLegit) {
                pendingAction = () -> executeLegitMode(-1);
            } else {
                executePacketMode();
            }

            forceUse = false;
        }


        public void handleUse(int bind, boolean isLegit) {
            if (!isLegit) {
                pendingAction = null;
            }

            if (mc.currentScreen == null && pendingAction != null) {
                pendingAction.run();
            }

            if (!KeyStorage.isPressed(bind) && mc.currentScreen == null) {
                forceUse = false;
                return;
            }

            if (forceUse || mc.currentScreen != null || mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) return;

            if (isLegit) {
                pendingAction = () -> executeLegitMode(bind);
            } else {
                executePacketMode();
            }
        }

        public void executePacketMode() {
            int invSlot = InventoryUtil.findItem(item, false);
            int hbSlot = InventoryUtil.findItem(item, true);

            if (mc.player.getOffHandStack().isOf(item)) {
                applyRotation();
                InventoryUtil.useItem(Hand.OFF_HAND);
                forceUse = true;
                return;
            }

            if (mc.player.getMainHandStack().isOf(item)) {
                applyRotation();
                InventoryUtil.useItem(Hand.MAIN_HAND);
                forceUse = true;
                return;
            }

            int oldSlot = mc.player.getInventory().selectedSlot;
            int bestSlot = InventoryUtil.findBestSlotInHotBar();

            if (hbSlot != -1) {
                applyRotation();
                InventoryUtil.swapToSlot(hbSlot);
                InventoryUtil.useItem(Hand.MAIN_HAND);
                InventoryUtil.swapToSlot(oldSlot);
                forceUse = true;
            } else if (invSlot != -1) {
                Runnable runnable = () -> {
                    applyRotation();
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    InventoryUtil.swapToSlot(bestSlot);
                    InventoryUtil.useItem(Hand.MAIN_HAND);
                    InventoryUtil.swapToSlot(oldSlot);
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    forceUse = true;
                };

                if (SlownessManager.isEnabled()) {
                    SlownessManager.applySlowness(10, runnable);
                } else {
                    runnable.run();
                }
            }
        }

        public void executeLegitMode(int bind) {
            switch (usageState) {
                case IDLE:
                    if (bind != -1 && !KeyStorage.isPressed(bind)) return;

                    targetSlot = InventoryUtil.findItem(item, true);
                    previousSlot = mc.player.getInventory().selectedSlot;

                    if (targetSlot == -1) {
                        targetSlot = InventoryUtil.findItem(item, false);
                        if (targetSlot == -1) return;
                        usageState = UsageState.SWAP_ITEM;
                        stateDelay = 1;
                    } else {
                        mc.player.getInventory().selectedSlot = targetSlot;
                        usageState = UsageState.USE_ITEM;
                        stateDelay = 1;
                    }
                    break;

                case SWAP_ITEM:
                    if (stateDelay-- > 0) return;

                    int hotbarSlot = InventoryUtil.findBestSlotInHotBar();
                    if (hotbarSlot != -1) {
                        Runnable toUseItem = () -> {
                            InventoryUtil.swapSlots(targetSlot, hotbarSlot);
                            targetSlot = hotbarSlot;
                            mc.player.getInventory().selectedSlot = targetSlot;
                            InventoryUtil.swapToSlot(targetSlot);
                            usageState = UsageState.USE_ITEM;
                            stateDelay = 1;
                        };
                        if (SlownessManager.isEnabled()) {
                            SlownessManager.applySlowness(10, toUseItem);
                        } else {
                            toUseItem.run();
                        }
                    } else {
                        usageState = UsageState.IDLE;
                    }
                    break;

                case USE_ITEM:
                    if (stateDelay-- > 0) return;

                    applyRotation();
                    InventoryUtil.useItem(Hand.MAIN_HAND);
                    usageState = UsageState.RESTORE_SLOT;
                    stateDelay = 1;
                    break;

                case RESTORE_SLOT:
                    if (stateDelay-- > 0) return;

                    mc.player.getInventory().selectedSlot = previousSlot;
                    usageState = UsageState.IDLE;
                    pendingAction = null;
                    forceUse = true;
                    break;
            }
        }

        public enum UsageState {
            IDLE,
            SWAP_ITEM,
            USE_ITEM,
            RESTORE_SLOT
        }
    }
}
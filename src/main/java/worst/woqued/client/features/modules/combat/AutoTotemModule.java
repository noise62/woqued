package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.client.PacketEvent;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.module.setting.MultiBooleanSetting;
import worst.woqued.api.utils.math.TimerUtil;
import worst.woqued.api.utils.player.InventoryUtil;
import worst.woqued.api.utils.other.SlownessManager;

import java.util.stream.IntStream;

@ModuleRegister(name = "Auto Totem", category = Category.COMBAT)
public class AutoTotemModule extends Module {
    @Getter private static final AutoTotemModule instance = new AutoTotemModule();

    // Settings from TROLLHACK
    private final MultiBooleanSetting options = new MultiBooleanSetting("Options").value(
            new BooleanSetting("Elytra Health").value(true),
            new BooleanSetting("TNT").value(true),
            new BooleanSetting("Falling").value(false),
            new BooleanSetting("End Crystal").value(false)
    );
    private final SliderSetting health = new SliderSetting("Health").value(4f).range(1f, 20f).step(0.5f);
    private final SliderSetting elytraHealth = new SliderSetting("Elytra Health").value(9f).range(0f, 20f).step(0.5f)
            .setVisible(() -> options.isEnabled("Elytra Health"));
    private final SliderSetting crystalDistance = new SliderSetting("Crystal Distance").value(4f).range(1f, 10f).step(1f)
            .setVisible(() -> options.isEnabled("End Crystal"));
    private final SliderSetting tntDistance = new SliderSetting("TNT Distance").value(30f).range(3f, 50f).step(1f)
            .setVisible(() -> options.isEnabled("TNT"));
    private final BooleanSetting noBall = new BooleanSetting("No Ball Swap").value(false);

    // State variables
    private int oldSlot = -1;
    private ItemStack oldOffhandItem = ItemStack.EMPTY;
    private int nonEnchantedTotems;
    private final TimerUtil swapTimer = new TimerUtil();
    private boolean lockHeld;
    private boolean totemIsUsed = false;

    public AutoTotemModule() {
        addSettings(options, health, elytraHealth, crystalDistance, tntDistance, noBall);
    }

    @Override
    public void onDisable() {
        if (lockHeld) {
            lockHeld = false;
        }
        resetSwapBack();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player != null && mc.player.isAlive() && mc.world != null) {
                updateTotemCount();
                bypass();
            } else {
                if (lockHeld) {
                    lockHeld = false;
                    resetSwapBack();
                }
            }
        }));

        addEvents(updateEvent);
    }

    private void updateTotemCount() {
        nonEnchantedTotems = (int) IntStream.range(0, 36)
                .mapToObj(i -> mc.player.getInventory().getStack(i))
                .filter(s -> s.getItem() == Items.TOTEM_OF_UNDYING && !s.hasEnchantments())
                .count();
    }

    private void bypass() {
        int slot = findNonEnchantedTotemSlot();
        boolean totemInHand = isTotemInHands();

        if (canSwap()) {
            if (slot >= 0 && !totemInHand) {
                if (!lockHeld && mc.currentScreen == null) {
                    // Stop movement like in TROLLHACK
                    mc.options.forwardKey.setPressed(false);
                    mc.player.setSprinting(false);
                    mc.options.sprintKey.setPressed(false);

                    if (!isMoving()) {
                        if (oldOffhandItem.isEmpty() && !mc.player.getOffHandStack().isEmpty()) {
                            oldOffhandItem = mc.player.getOffHandStack().copy();
                            oldSlot = slot;
                        }

                        // Swap totem to offhand
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
                        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
                        lockHeld = true;
                        swapTimer.reset();
                    }
                }

                if (lockHeld && swapTimer.finished(50)) {
                    lockHeld = false;
                    return;
                }
            }
        } else if (oldSlot != -1 && !oldOffhandItem.isEmpty()) {
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                if (!lockHeld && mc.currentScreen == null) {
                    // Stop movement
                    mc.options.forwardKey.setPressed(false);
                    mc.player.setSprinting(false);
                    mc.options.sprintKey.setPressed(false);

                    // Swap back original item
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, oldSlot, 40, SlotActionType.SWAP, mc.player);
                    mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
                    lockHeld = true;
                    swapTimer.reset();
                }

                if (lockHeld && swapTimer.finished(50)) {
                    lockHeld = false;
                    resetSwapBack();
                    return;
                }
            } else {
                resetSwapBack();
            }
        } else if (lockHeld) {
            lockHeld = false;
            resetSwapBack();
        }
    }

    private boolean isMoving() {
        return mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
    }

    private void resetSwapBack() {
        oldOffhandItem = ItemStack.EMPTY;
        oldSlot = -1;
    }

    private int findNonEnchantedTotemSlot() {
        // First try to find non-enchanted totem
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING && !stack.hasEnchantments()) {
                return i < 9 ? i + 36 : i;
            }
        }

        // If no non-enchanted found, use any totem
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i < 9 ? i + 36 : i;
            }
        }

        return -1;
    }

    public boolean isTotemInHands() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();

        if (mainHand.getItem() == Items.TOTEM_OF_UNDYING) {
            return !mainHand.hasEnchantments() || nonEnchantedTotems <= 0;
        } else {
            if (offHand.getItem() != Items.TOTEM_OF_UNDYING) {
                return false;
            }
            return !offHand.hasEnchantments() || nonEnchantedTotems <= 0;
        }
    }

    private boolean canSwap() {
        boolean flag1 = elytraCheck();
        boolean flag2 = checkCrystal();
        boolean flag3 = checkTnt();
        boolean flag4 = checkFall();
        boolean flag6 = mc.player.getHealth() + getAbsorption() <= health.getValue();
        return flag1 || flag2 || flag3 || flag4 || flag6;
    }

    private boolean elytraCheck() {
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean elytra = chestStack.getItem() == Items.ELYTRA && options.isEnabled("Elytra Health");
        return elytra && checkHealth();
    }

    private boolean checkFall() {
        if (!options.isEnabled("Falling")) {
            return false;
        }
        return mc.player.fallDistance > 2.0;
    }

    private boolean checkHealth() {
        return mc.player.getHealth() + getAbsorption() <= elytraHealth.getValue();
    }

    private boolean checkCrystal() {
        if (!options.isEnabled("End Crystal")) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= crystalDistance.getValue()) {
                return !(mc.player.getOffHandStack().getItem() instanceof PlayerHeadItem) || !noBall.getValue();
            }
        }

        return false;
    }

    private boolean checkTnt() {
        if (!options.isEnabled("TNT")) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            float distance = mc.player.distanceTo(entity);
            if ((entity instanceof TntEntity || entity instanceof TntMinecartEntity)
                    && distance <= tntDistance.getValue()) {
                return true;
            }
        }

        return false;
    }

    private float getAbsorption() {
        return mc.player.getAbsorptionAmount();
    }
}

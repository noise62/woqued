package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BindSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.math.MathUtil;
import worst.woqued.api.utils.player.InventoryUtil;

@ModuleRegister(name = "Auto Cart", category = Category.COMBAT)
public class AutoCartModule extends Module {
    @Getter private static final AutoCartModule instance = new AutoCartModule();

    private final BindSetting activateKey = new BindSetting("Activate Key").value(-1);
    private final SliderSetting bowCharge = new SliderSetting("Bow Charge").value(30f).range(5f, 60f).step(1f);

    private enum State {
        IDLE,
        PLACING_RAIL,
        PLACING_MINECART,
        DRAWING_BOW,
        SHOOTING,
        FIRING,
        DONE
    }

    private State currentState = State.IDLE;
    private int actionTimer = 0;
    private int originalSlot = 0;
    private int railSlot = -1;
    private int bowSlot = -1;
    private int minecartSlot = -1;
    private BlockHitResult targetHit = null;
    private boolean bowStarted = false;
    private float originalYaw = 0;
    private float originalPitch = 0;
    private float targetYaw = 0;
    private float targetPitch = 0;

    public AutoCartModule() {
        addSettings(activateKey, bowCharge);
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = worst.woqued.api.event.events.client.KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isEnabled()) return;
            if (mc.player == null || mc.world == null) return;
            if (mc.currentScreen != null) return;
            if (currentState != State.IDLE) return;
            if (event.action() != 1) return;

            int key = activateKey.getValue();
            if (key == -1 || key == -999) return;

            if (event.key() == key) {
                execute();
            }
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            if (currentState != State.IDLE) {
                updateRotation();
                processTick();
            }
        }));

        addEvents(keyEvent, updateEvent);
    }

    private void execute() {
        PlayerInventory inventory = mc.player.getInventory();

        minecartSlot = findMinecart(inventory);
        railSlot = findRail(inventory);
        bowSlot = findBow(inventory);

        if (minecartSlot == -1 || railSlot == -1 || bowSlot == -1) {
            return;
        }

        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        targetHit = (BlockHitResult) hit;
        originalSlot = inventory.selectedSlot;
        originalYaw = mc.player.getYaw();
        originalPitch = mc.player.getPitch();
        actionTimer = 0;
        bowStarted = false;

        calculateTargetRotation();
        currentState = State.PLACING_RAIL;
    }

    private void calculateTargetRotation() {
        Vec3d playerPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        Vec3d blockPos = Vec3d.ofCenter(targetHit.getBlockPos()).add(0, 0.7, 0);

        double deltaX = blockPos.x - playerPos.x;
        double deltaY = blockPos.y - playerPos.y;
        double deltaZ = blockPos.z - playerPos.z;

        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
    }

    private void updateRotation() {
        if (currentState != State.IDLE && currentState != State.DONE) {
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(targetPitch);
            mc.player.prevYaw = targetYaw;
            mc.player.prevPitch = targetPitch;
        }
    }

    private void processTick() {
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            reset();
            return;
        }

        actionTimer++;

        try {
            processFastMode();
        } catch (Exception ex) {
            reset();
        }
    }

    private void processFastMode() {
        switch (currentState) {
            case PLACING_RAIL -> {
                InventoryUtil.swapToSlot(railSlot);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, targetHit);
                currentState = State.PLACING_MINECART;
                actionTimer = 0;
            }
            case PLACING_MINECART -> {
                InventoryUtil.swapToSlot(minecartSlot);
                mc.options.useKey.setPressed(true);
                currentState = State.DRAWING_BOW;
                actionTimer = 0;
            }
            case DRAWING_BOW -> {
                mc.options.useKey.setPressed(false);
                InventoryUtil.swapToSlot(bowSlot);
                currentState = State.SHOOTING;
                actionTimer = 0;
            }
            case SHOOTING -> {
                if (actionTimer >= 1) {
                    mc.options.useKey.setPressed(true);
                    bowStarted = true;
                    currentState = State.FIRING;
                    actionTimer = 0;
                }
            }
            case FIRING -> {
                if (actionTimer >= bowCharge.getValue()) {
                    if (bowStarted) {
                        mc.options.useKey.setPressed(false);
                        bowStarted = false;
                    }
                    currentState = State.DONE;
                    actionTimer = 0;
                }
            }
            case DONE -> {
                if (actionTimer >= 1) {
                    mc.player.setYaw(originalYaw);
                    mc.player.setPitch(originalPitch);
                    InventoryUtil.swapToSlot(originalSlot);
                    reset();
                }
            }
        }
    }

    private void reset() {
        if (bowStarted && mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
        currentState = State.IDLE;
        actionTimer = 0;
        bowStarted = false;
        targetHit = null;
    }

    private int findMinecart(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.TNT_MINECART) {
                return i;
            }
        }
        return -1;
    }

    private int findRail(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();
            if (itemId.contains("rail")) {
                return i;
            }
        }
        return -1;
    }

    private int findBow(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.BOW) {
                return i;
            }
        }
        return -1;
    }
}
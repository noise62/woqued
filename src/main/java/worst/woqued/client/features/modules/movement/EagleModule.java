package worst.woqued.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationStrategy;
import worst.woqued.api.utils.task.TaskPriority;

@ModuleRegister(name = "Eagle", category = Category.MOVEMENT)
public class EagleModule extends Module {
    @Getter private static final EagleModule instance = new EagleModule();

    private long enableTimeMs = 0L;
    private float stableYaw = 180.0f;
    private int oldSlot = -1;
    private int lockedBlockSlot = -1;

    public EagleModule() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null) return;
        oldSlot = mc.player.getInventory().selectedSlot;
        stableYaw = getMovementBasedYaw();
        lockedBlockSlot = -1;
        enableTimeMs = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.getInventory().selectedSlot = oldSlot;
            mc.options.sneakKey.setPressed(false);
        }
        oldSlot = -1;
        lockedBlockSlot = -1;
        enableTimeMs = 0L;
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));

        addEvents(updateEvent);
    }

    private void onUpdate() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        stableYaw = getMovementBasedYaw();
        float targetYaw = stableYaw;
        float targetPitch = calculateDynamicPitch();

        RotationManager.getInstance().addRotation(
                new Rotation(targetYaw, targetPitch),
                RotationStrategy.TARGET,
                TaskPriority.LOW,
                this
        );

        updateSneakState();
        tryPlaceLegit();
    }

    private void updateSneakState() {
        if (mc.player == null) return;

        BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.1, mc.player.getZ());
        boolean onGround = !mc.world.getBlockState(feet).isAir();

        if (onGround) {
            mc.options.sneakKey.setPressed(false);
            return;
        }

        double localX = mc.player.getX() - feet.getX();
        double localZ = mc.player.getZ() - feet.getZ();

        double minToEdge = Math.min(
                Math.min(localX, 1 - localX),
                Math.min(localZ, 1 - localZ)
        );

        mc.options.sneakKey.setPressed(minToEdge < 0.30);
    }

    private void tryPlaceLegit() {
        if (mc.interactionManager == null || mc.player == null) return;
        if (System.currentTimeMillis() - enableTimeMs < 180L) return;
        if (mc.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR || mc.player.isUsingItem()) return;

        HitResult crosshair = mc.crosshairTarget;
        if (!(crosshair instanceof BlockHitResult hitResult) || crosshair.getType() != HitResult.Type.BLOCK) return;
        if (!isValidPlaceRaycast(hitResult)) return;

        int slot = getBlockSlot();
        if (slot == -1) return;

        if (oldSlot == -1) oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        KeyBinding.onKeyPressed(mc.options.useKey.getDefaultKey());
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isValidPlaceRaycast(BlockHitResult hitResult) {
        if (mc.player == null || mc.world == null) return false;
        BlockPos supportPos = hitResult.getBlockPos();
        if (mc.world.getBlockState(supportPos).isAir()) return false;
        if (!mc.world.getFluidState(supportPos).isEmpty()) return false;
        BlockPos placePos = supportPos.offset(hitResult.getSide());
        if (!mc.world.getBlockState(placePos).isReplaceable()) return false;
        BlockHitResult trace = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(),
                hitResult.getPos(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        if (trace.getType() != HitResult.Type.BLOCK) {
            return true;
        }
        BlockPos traced = trace.getBlockPos();
        return traced.equals(supportPos) || traced.equals(placePos);
    }

    private int getBlockSlot() {
        if (mc.player == null) return -1;
        if (isValidBlockSlot(lockedBlockSlot)) return lockedBlockSlot;
        int selectedSlot = mc.player.getInventory().selectedSlot;
        if (isValidBlockSlot(selectedSlot)) {
            lockedBlockSlot = selectedSlot;
            return lockedBlockSlot;
        }
        int bestSlot = -1;
        int bestCount = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof BlockItem)) continue;
            if (stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestSlot = i;
            }
        }
        lockedBlockSlot = bestSlot;
        return bestSlot;
    }

    private boolean isValidBlockSlot(int slot) {
        if (slot < 0 || slot > 8) return false;
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getInventory().getStack(slot);
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    private float calculateDynamicPitch() {
        if (mc.player == null) return 80.0f;
        BlockPos standing = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1.0, mc.player.getZ());
        double localX = mc.player.getX() - standing.getX();
        double localZ = mc.player.getZ() - standing.getZ();
        double west = localX;
        double east = 1.0 - localX;
        double north = localZ;
        double south = 1.0 - localZ;
        double min = Math.min(Math.min(west, east), Math.min(north, south));
        float basePitch = 78.0f;
        if (min < 0.2) {
            float pitchAdjustment = (float) ((0.2 - min) * 15);
            return MathHelper.clamp(basePitch + pitchAdjustment, 72.0f, 86.0f);
        }
        return MathHelper.clamp(basePitch, -89.0f, 89.0f);
    }

    private float getMovementBasedYaw() {
        if (mc.player == null) return 180.0f;
        Vec3d movementInput = getMovementInput();
        if (movementInput.lengthSquared() < 0.01) {
            return stableYaw;
        }
        double moveX = movementInput.x;
        double moveZ = movementInput.z;
        if (Math.abs(moveX) > Math.abs(moveZ)) {
            if (moveX > 0) return 90.0f;
            else return -90.0f;
        } else {
            if (moveZ > 0) return -180.0f;
            else return 0.0f;
        }
    }

    private Vec3d getMovementInput() {
        if (mc.player == null) return Vec3d.ZERO;
        float forward = 0;
        float sideways = 0;
        if (mc.options.forwardKey.isPressed()) forward += 1;
        if (mc.options.backKey.isPressed()) forward -= 1;
        if (mc.options.leftKey.isPressed()) sideways += 1;
        if (mc.options.rightKey.isPressed()) sideways -= 1;
        float yawRad = mc.player.getYaw() * (float) (Math.PI / 180.0);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double moveX = sideways * cos - forward * sin;
        double moveZ = forward * cos + sideways * sin;
        return new Vec3d(moveX, 0, moveZ);
    }
}
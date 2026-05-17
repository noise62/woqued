package worst.woqued.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationStrategy;
import worst.woqued.api.utils.rotation.rotations.ScaffoldRotation;
import worst.woqued.api.utils.task.TaskPriority;

@ModuleRegister(name = "Scaffold", category = Category.MOVEMENT)
public class ScaffoldModule extends Module {
    @Getter private static final ScaffoldModule instance = new ScaffoldModule();

    private int oldSlot = -1;
    private int lockedBlockSlot = -1;
    private BlockPos lastTargetPos = null;
    private Direction lastTargetSide = null;
    private Vec3d fixedLookTarget = null;
    private boolean isLocked = false;

    @Getter private final SliderSetting placeDistance = new SliderSetting("Place distance").value(4.5f).range(1f, 6f).step(0.1f);
    @Getter private final BooleanSetting autoSneak = new BooleanSetting("Auto sneak").value(true);

    public ScaffoldModule() {
        addSettings(placeDistance, autoSneak);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null) return;
        oldSlot = mc.player.getInventory().selectedSlot;
        lockedBlockSlot = -1;
        fixedLookTarget = null;
        isLocked = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.getInventory().selectedSlot = oldSlot;
        }
        oldSlot = -1;
        lockedBlockSlot = -1;
        lastTargetPos = null;
        lastTargetSide = null;
        fixedLookTarget = null;
        isLocked = false;
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        addEvents(updateEvent);
    }

    private void onUpdate() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        handleRotation();

        if (autoSneak.getValue()) {
            updateSneakState();
        }

        tryPlaceBlock();
    }

    private void handleRotation() {
        if (!isLocked) {
            Vec3d target = findPlaceTarget();
            if (target == null) {
                return;
            }
            fixedLookTarget = target;
            isLocked = true;
        }

        if (fixedLookTarget == null) {
            return;
        }

        Rotation rotation = RotationUtil.fromVec3d(fixedLookTarget.subtract(mc.player.getEyePos()));

        RotationStrategy strategy = new RotationStrategy(new ScaffoldRotation(), true, false);

        RotationManager.getInstance().addRotation(
                new Rotation(rotation.getYaw(), rotation.getPitch()),
                strategy,
                TaskPriority.HIGH,
                this
        );
    }

    private Vec3d findPlaceTarget() {
        Direction moveDir = getMovementDirection();
        if (moveDir == null) {
            return null;
        }

        BlockPos feet = BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        BlockPos targetPos = feet.offset(moveDir);

        if (mc.world.getBlockState(targetPos).isAir() || mc.world.getBlockState(targetPos).isReplaceable()) {
            BlockPos supportPos = targetPos.down();
            if (mc.world.getBlockState(supportPos).isSolid()) {
                lastTargetPos = targetPos;
                lastTargetSide = moveDir;
                return getEdgePosition(targetPos, moveDir);
            }
        }

        return null;
    }

    private Vec3d getEdgePosition(BlockPos blockPos, Direction dir) {
        double edgeX, edgeY, edgeZ;

        switch (dir) {
            case EAST -> {
                edgeX = blockPos.getX() + 0.01;
                edgeZ = blockPos.getZ() + 0.5;
                edgeY = blockPos.getY() + 0.02;
            }
            case WEST -> {
                edgeX = blockPos.getX() + 0.99;
                edgeZ = blockPos.getZ() + 0.5;
                edgeY = blockPos.getY() + 0.02;
            }
            case SOUTH -> {
                edgeX = blockPos.getX() + 0.5;
                edgeZ = blockPos.getZ() + 0.01;
                edgeY = blockPos.getY() + 0.02;
            }
            case NORTH -> {
                edgeX = blockPos.getX() + 0.5;
                edgeZ = blockPos.getZ() + 0.99;
                edgeY = blockPos.getY() + 0.02;
            }
            default -> {
                edgeX = blockPos.getX() + 0.5;
                edgeZ = blockPos.getZ() + 0.5;
                edgeY = blockPos.getY() + 0.02;
            }
        }

        return new Vec3d(edgeX, edgeY, edgeZ);
    }

    private Direction getMovementDirection() {
        if (mc.player == null) return null;

        boolean forward = mc.options.forwardKey.isPressed();
        boolean backward = mc.options.backKey.isPressed();
        boolean left = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();

        if (forward && !backward) {
            if (left && !right) return Direction.WEST;
            if (right && !left) return Direction.EAST;
            return Direction.SOUTH;
        }
        if (backward && !forward) {
            if (left && !right) return Direction.EAST;
            if (right && !left) return Direction.WEST;
            return Direction.NORTH;
        }
        if (left && !right) return Direction.WEST;
        if (right && !left) return Direction.EAST;

        return null;
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

    private void tryPlaceBlock() {
        if (mc.interactionManager == null || mc.player == null) return;
        if (mc.player.isUsingItem()) return;

        if (lastTargetPos == null) return;

        BlockPos supportBlock = lastTargetPos.down();
        if (!mc.world.getBlockState(supportBlock).isSolid()) return;

        int slot = getBlockSlot();
        if (slot == -1) return;

        if (oldSlot == -1) oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        Hand hand = Hand.MAIN_HAND;
        ItemStack stack = mc.player.getStackInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            hand = Hand.OFF_HAND;
            stack = mc.player.getStackInHand(hand);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                return;
            }
        }

        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d targetPos = fixedLookTarget != null ? fixedLookTarget : Vec3d.ofCenter(lastTargetPos);

        BlockHitResult hitResult = mc.world.raycast(new RaycastContext(
                playerEyePos,
                targetPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (hitResult == null || hitResult.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
            hitResult = BlockHitResult.createMissed(
                    net.minecraft.util.math.Vec3d.ofCenter(lastTargetPos),
                    lastTargetSide != null ? lastTargetSide : Direction.DOWN,
                    lastTargetPos
            );
        }

        mc.crosshairTarget = hitResult;

        ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, hitResult);

        if (result.isAccepted()) {
            mc.player.swingHand(hand);
            isLocked = false;
        }
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
}
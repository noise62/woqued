package worst.woqued.client.features.modules.other;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

@ModuleRegister(name = "Apple Farm", category = Category.OTHER)
public class AutoAppleFarmModule extends Module {
    private static final AutoAppleFarmModule INSTANCE = new AutoAppleFarmModule();

    private enum State {
        PLACING_SAPLING,
        GROWING_TREE,
        BREAKING_LOG,
        BREAKING_LEAVES
    }

    private State currentState = State.PLACING_SAPLING;
    private BlockPos treePos = null;
    private int saplingSlot = -1;
    private int boneMealSlot = -1;
    private int axeSlot = -1;
    private int hoeSlot = -1;
    private BlockPos lastLookPos = null;
    private int boneMealTries = 0;

    public AutoAppleFarmModule() {
    }

    public static AutoAppleFarmModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

            updateSlots();

            switch (currentState) {
                case PLACING_SAPLING -> placeSapling();
                case GROWING_TREE -> growTree();
                case BREAKING_LOG -> breakLog();
                case BREAKING_LEAVES -> breakLeaves();
            }
        }));

        addEvents(tickEvent);
    }

    private void updateSlots() {
        saplingSlot = -1;
        boneMealSlot = -1;
        axeSlot = -1;
        hoeSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() == Items.OAK_SAPLING) {
                saplingSlot = i;
            } else if (stack.getItem() == Items.BONE_MEAL) {
                boneMealSlot = i;
            } else {
                String name = stack.getItem().toString();
                if (name.toLowerCase().contains("axe") && !name.toLowerCase().contains("pick")) {
                    axeSlot = i;
                }
                if (name.toLowerCase().contains("hoe")) {
                    hoeSlot = i;
                }
            }
        }
    }

    private void placeSapling() {
        mc.options.attackKey.setPressed(false);

        if (saplingSlot == -1 || boneMealSlot == -1 || axeSlot == -1 || hoeSlot == -1) return;

        BlockPos groundPos = findGroundPos();
        if (groundPos == null) return;

        var block = mc.world.getBlockState(groundPos).getBlock();
        if (block == Blocks.OAK_LOG || block == Blocks.OAK_LEAVES) {
            currentState = State.BREAKING_LOG;
            return;
        }

        if (mc.world.getBlockState(groundPos).isAir()) {
            mc.player.getInventory().selectedSlot = saplingSlot;

            if (!groundPos.equals(lastLookPos)) {
                lookAt(groundPos);
                lastLookPos = groundPos;
            }

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(groundPos), Direction.UP, groundPos, false));

            treePos = groundPos;
            currentState = State.GROWING_TREE;
            boneMealTries = 0;
        } else {
            treePos = groundPos;
            currentState = State.GROWING_TREE;
        }
    }

    private void growTree() {
        if (treePos == null) {
            currentState = State.PLACING_SAPLING;
            return;
        }

        if (mc.world.getBlockState(treePos).getBlock() == Blocks.OAK_SAPLING) {
            mc.player.getInventory().selectedSlot = boneMealSlot;

            if (!treePos.equals(lastLookPos)) {
                lookAt(treePos);
                lastLookPos = treePos;
            }

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(treePos), Direction.UP, treePos, true));

            boneMealTries++;
            if (boneMealTries > 5) {
                currentState = State.BREAKING_LEAVES;
            }
        } else {
            currentState = State.BREAKING_LOG;
        }
    }

    private void breakLeaves() {
        BlockPos leafPos = findBlockFast(Blocks.OAK_LEAVES);

        if (leafPos == null) {
            mc.options.attackKey.setPressed(false);
            currentState = State.PLACING_SAPLING;
            treePos = null;
            lastLookPos = null;
            return;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String name = stack.getItem().toString().toLowerCase();
                if (name.contains("hoe")) {
                    mc.player.getInventory().selectedSlot = i;
                    break;
                }
            }
        }

        lookAt(leafPos);
        lastLookPos = leafPos;

        mc.options.attackKey.setPressed(true);
    }

    private void breakLog() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String name = stack.getItem().toString().toLowerCase();
                if (name.contains("axe") && !name.contains("pick")) {
                    mc.player.getInventory().selectedSlot = i;
                    break;
                }
            }
        }

        BlockPos logPos = findBlockFast(Blocks.OAK_LOG);

        if (logPos != null) {
            if (!logPos.equals(lastLookPos)) {
                lookAt(logPos);
                lastLookPos = logPos;
            }

            mc.options.attackKey.setPressed(true);
        } else {
            mc.options.attackKey.setPressed(false);
            currentState = State.BREAKING_LEAVES;
            lastLookPos = null;
        }
    }

    private BlockPos findGroundPos() {
        int px = (int) mc.player.getX();
        int py = (int) mc.player.getY() - 1;
        int pz = (int) mc.player.getZ();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = new BlockPos(px + x, py, pz + z);
                var block = mc.world.getBlockState(pos).getBlock();
                if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT ||
                        block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT) {
                    return pos.up();
                }
            }
        }
        return null;
    }

    private BlockPos findBlockFast(net.minecraft.block.Block block) {
        int px = (int) mc.player.getX();
        int py = (int) mc.player.getY();
        int pz = (int) mc.player.getZ();

        double minDist = 5.0;
        BlockPos nearest = null;

        for (int y = -1; y <= 3; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = new BlockPos(px + dx, py + y, pz + dz);
                    if (mc.world.getBlockState(pos).getBlock() == block) {
                        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private void lookAt(BlockPos pos) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d target = Vec3d.ofCenter(pos);

        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        mc.player.setPitch((float) Math.toDegrees(Math.atan2(-dy, dist)));
    }

    @Override
    public void onDisable() {
        mc.options.attackKey.setPressed(false);
        currentState = State.PLACING_SAPLING;
        treePos = null;
        lastLookPos = null;
    }
}
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
import worst.woqued.api.utils.other.TextUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "Apple Farm", category = Category.OTHER)
public class AutoAppleFarmModule extends Module {
    private static final AutoAppleFarmModule INSTANCE = new AutoAppleFarmModule();

    private enum State {
        PLACING_SAPLING,
        GROWING_TREE,
        BREAKING_LEAVES,
        BREAKING_LOG,
        WAITING
    }

    private State currentState = State.WAITING;
    private BlockPos treePos = null;
    private ScheduledExecutorService executor;
    private volatile boolean running = false;
    private boolean isBreaking = false;
    private BlockPos currentBreakingPos = null;

    public AutoAppleFarmModule() {
    }

    @Override
    public void onEvent() {
    }

    public static AutoAppleFarmModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        if (running) return;

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        currentState = State.PLACING_SAPLING;
        treePos = null;
        running = true;
        isBreaking = false;
        currentBreakingPos = null;

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (mc == null || mc.player == null || mc.world == null) return;
        if (!running) {
            if (isBreaking) {
                mc.options.attackKey.setPressed(false);
                isBreaking = false;
            }
            return;
        }

        if (!checkInventory()) return;

        mc.execute(() -> {
            autoSelectTool();

            switch (currentState) {
                case PLACING_SAPLING -> placeSapling();
                case GROWING_TREE -> growTree();
                case BREAKING_LEAVES -> breakLeaves();
                case BREAKING_LOG -> breakLog();
                case WAITING -> {}
            }
        });
    }

    private void autoSelectTool() {
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos targetPos = hitResult.getBlockPos();

            if (targetPos != null && mc.world != null) {
                net.minecraft.block.Block targetBlock = mc.world.getBlockState(targetPos).getBlock();

                if (targetBlock == Blocks.OAK_LOG || targetBlock == Blocks.BIRCH_LOG ||
                        targetBlock == Blocks.SPRUCE_LOG || targetBlock == Blocks.JUNGLE_LOG ||
                        targetBlock == Blocks.ACACIA_LOG || targetBlock == Blocks.DARK_OAK_LOG ||
                        targetBlock == Blocks.CHERRY_LOG || targetBlock == Blocks.MANGROVE_LOG) {
                    int axeSlot = findToolSlot("axe");
                    if (axeSlot != -1) {
                        mc.player.getInventory().selectedSlot = axeSlot;
                    }
                } else if (targetBlock == Blocks.OAK_LEAVES || targetBlock == Blocks.BIRCH_LEAVES ||
                        targetBlock == Blocks.SPRUCE_LEAVES || targetBlock == Blocks.JUNGLE_LEAVES ||
                        targetBlock == Blocks.ACACIA_LEAVES || targetBlock == Blocks.DARK_OAK_LEAVES ||
                        targetBlock == Blocks.CHERRY_LEAVES || targetBlock == Blocks.MANGROVE_LEAVES) {
                    int hoeSlot = findToolSlot("hoe");
                    if (hoeSlot != -1) {
                        mc.player.getInventory().selectedSlot = hoeSlot;
                    }
                }
            }
        }
    }

    private boolean checkInventory() {
        boolean hasSapling = false;
        boolean hasBoneMeal = false;
        boolean hasAxe = false;
        boolean hasHoe = false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.OAK_SAPLING) hasSapling = true;
                if (stack.getItem() == Items.BONE_MEAL) hasBoneMeal = true;

                String name = stack.getItem().toString().toLowerCase();
                if (name.contains("axe") && !name.contains("pick")) hasAxe = true;
                if (name.contains("hoe")) hasHoe = true;
            }
        }

        if (!hasSapling) {
            TextUtil.sendMessage("Отсутствует саженец дуба");
            return false;
        }
        if (!hasBoneMeal) {
            TextUtil.sendMessage("Отсутствует костная мука");
            return false;
        }
        if (!hasAxe) {
            TextUtil.sendMessage("Отсутствует топор");
            return false;
        }
        if (!hasHoe) {
            TextUtil.sendMessage("Отсутствует мотыга");
            return false;
        }

        return true;
    }

    private void placeSapling() {
        if (isBreaking) {
            stopBreaking();
        }

        BlockPos groundPos = findGroundPos();
        if (groundPos == null) return;

        if (mc.world.getBlockState(groundPos).getBlock() == Blocks.OAK_LOG ||
                mc.world.getBlockState(groundPos).getBlock() == Blocks.OAK_LEAVES) {
            currentState = State.BREAKING_LEAVES;
            return;
        }

        if (mc.world.getBlockState(groundPos).isAir()) {
            int saplingSlot = findItemSlot(Items.OAK_SAPLING);

            if (saplingSlot != -1) {
                mc.player.getInventory().selectedSlot = saplingSlot;
                lookAt(groundPos);

                BlockHitResult hit = new BlockHitResult(
                        Vec3d.ofCenter(groundPos), Direction.UP, groundPos, false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);

                treePos = groundPos;
                currentState = State.GROWING_TREE;

                TextUtil.sendMessage("§7[+] Саженец поставлен!");
            }
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
            int boneMealSlot = findItemSlot(Items.BONE_MEAL);

            if (boneMealSlot != -1) {
                mc.player.getInventory().selectedSlot = boneMealSlot;
                lookAt(treePos);

                BlockHitResult hit = new BlockHitResult(
                        Vec3d.ofCenter(treePos), Direction.UP, treePos, true
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            currentState = State.BREAKING_LEAVES;
            TextUtil.sendMessage("§7[+] Дерево выросло! Начинаем ломать...");
        }
    }

    private void breakLeaves() {
        BlockPos leafPos = findNearestBlock(Blocks.OAK_LEAVES);

        if (leafPos != null) {
            if (currentBreakingPos != leafPos) {
                if (isBreaking) {
                    stopBreaking();
                }
                lookAt(leafPos);
                mc.options.attackKey.setPressed(true);
                currentBreakingPos = leafPos;
                isBreaking = true;
            } else if (!isBreaking) {
                lookAt(leafPos);
                mc.options.attackKey.setPressed(true);
                currentBreakingPos = leafPos;
                isBreaking = true;
            }
        } else {
            if (isBreaking) {
                stopBreaking();
            }
            currentState = State.BREAKING_LOG;
        }
    }

    private void breakLog() {
        BlockPos logPos = findNearestBlock(Blocks.OAK_LOG);

        if (logPos != null) {
            if (currentBreakingPos != logPos) {
                if (isBreaking) {
                    stopBreaking();
                }
                lookAt(logPos);
                mc.options.attackKey.setPressed(true);
                currentBreakingPos = logPos;
                isBreaking = true;
            } else if (!isBreaking) {
                lookAt(logPos);
                mc.options.attackKey.setPressed(true);
                currentBreakingPos = logPos;
                isBreaking = true;
            }
        } else {
            if (isBreaking) {
                stopBreaking();
            }
            currentState = State.PLACING_SAPLING;
            treePos = null;
            TextUtil.sendMessage("§7[+] Дерево срублено! Ставим новое...");
        }
    }

    private void stopBreaking() {
        mc.options.attackKey.setPressed(false);
        isBreaking = false;
        currentBreakingPos = null;
    }

    private BlockPos findGroundPos() {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = new BlockPos(
                        (int) Math.floor(mc.player.getX()) + x,
                        (int) Math.floor(mc.player.getY()) - 1,
                        (int) Math.floor(mc.player.getZ()) + z
                );
                if (mc.world.getBlockState(pos).getBlock() == Blocks.GRASS_BLOCK ||
                        mc.world.getBlockState(pos).getBlock() == Blocks.DIRT ||
                        mc.world.getBlockState(pos).getBlock() == Blocks.COARSE_DIRT ||
                        mc.world.getBlockState(pos).getBlock() == Blocks.ROOTED_DIRT) {
                    return pos.up();
                }
            }
        }
        return null;
    }

    private BlockPos findNearestBlock(net.minecraft.block.Block block) {
        BlockPos nearest = null;
        double nearestDist = 8.0;

        int range = 7;
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = new BlockPos(
                            (int) Math.floor(mc.player.getX()) + x,
                            (int) Math.floor(mc.player.getY()) + y,
                            (int) Math.floor(mc.player.getZ()) + z
                    );
                    if (mc.world.getBlockState(pos).getBlock() == block) {
                        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
                        if (dist < nearestDist && dist <= 5.0) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findToolSlot(String tool) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String name = stack.getItem().toString().toLowerCase();
                if (tool.equals("axe") && name.contains("axe") && !name.contains("pick")) {
                    return i;
                }
                if (tool.equals("hoe") && name.contains("hoe")) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void lookAt(BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eyePos = mc.player.getEyePos();

        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, distance));
        pitch = Math.max(-90, Math.min(90, pitch));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    @Override
    public void onDisable() {
        if (isBreaking) {
            mc.options.attackKey.setPressed(false);
            isBreaking = false;
        }
        currentBreakingPos = null;

        running = false;
        currentState = State.WAITING;

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
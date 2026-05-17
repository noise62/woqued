package worst.woqued.client.features.modules.other;

import net.minecraft.block.Blocks;
import net.minecraft.block.CarrotsBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.utils.other.TextUtil;

@ModuleRegister(name = "Carrot Farm", category = Category.OTHER)
public class CarrotFarmModule extends Module {
    private static final CarrotFarmModule INSTANCE = new CarrotFarmModule();

    private int mealSlot = -1;
    private int carrotSlot = -1;
    private int hoeSlot = -1;
    private EventListener tickListener;
    private boolean errorShown = false;

    public CarrotFarmModule() {
    }

    @Override
    public void onEvent() {
    }

    public static CarrotFarmModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        errorShown = false;
        tickListener = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
    }

    private void onTick() {
        if (mc == null || mc.player == null || mc.world == null) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hitResult)) return;

        mealSlot = findItemSlot(Items.BONE_MEAL);
        carrotSlot = findItemSlot(Items.CARROT);
        hoeSlot = findHoeSlot();

        if (hoeSlot == -1) {
            if (!errorShown) {
                TextUtil.sendMessage("Отсутствует мотыга");
                errorShown = true;
            }
            return;
        }
        if (carrotSlot == -1) {
            if (!errorShown) {
                TextUtil.sendMessage("Отсутствует морковь");
                errorShown = true;
            }
            return;
        }
        if (mealSlot == -1) {
            if (!errorShown) {
                TextUtil.sendMessage("Отсутствует костная мука");
                errorShown = true;
            }
            return;
        }

        errorShown = false;

        BlockPos targetPos = hitResult.getBlockPos();
        var blockState = mc.world.getBlockState(targetPos);
        var block = blockState.getBlock();

        if (block == Blocks.FARMLAND) {
            mc.player.getInventory().selectedSlot = carrotSlot;
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else if (block == Blocks.CARROTS) {
            if (blockState.getBlock() instanceof CarrotsBlock carrotsBlock) {
                int age = carrotsBlock.getAge(blockState);
                
                if (age == 7) {
                    mc.player.getInventory().selectedSlot = hoeSlot;
                    mc.interactionManager.attackBlock(targetPos, hitResult.getSide());
                    mc.player.swingHand(Hand.MAIN_HAND);
                } else {
                    mc.player.getInventory().selectedSlot = mealSlot;
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findHoeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String name = stack.getItem().toString().toLowerCase();
                if (name.contains("hoe")) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        if (tickListener != null) {
            tickListener.unsubscribe();
            tickListener = null;
        }
    }
}
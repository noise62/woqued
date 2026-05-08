package worst.woqued.client.features.modules.movement.noslow.modes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import worst.woqued.api.utils.player.InventoryUtil;
import worst.woqued.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowFunTime extends NoSlowMode {
    private int funtimeCrossbowSlot = -1;
    private boolean funtimeSwapped = false;

    @Override
    public String getName() {
        return "Fun Time";
    }

    @Override
    public void onUpdate() {
        if (!mc.player.isUsingItem() || !isConsumable(mc.player.getActiveItem())) {
            restoreCrossbow();
        }
    }

    @Override
    public void onTick() {
        if (slowingCancel()) {
            handleCrossbowSwap();
        }
    }

    @Override
    public boolean slowingCancel() {
        if (!mc.player.isUsingItem()) return false;

        ItemStack active = mc.player.getActiveItem();
        if (!isConsumable(active)) return false;

        if (InventoryUtil.findItem(Items.CROSSBOW, false) == -1) return false;

        mc.player.setSprinting(
                !mc.player.isGliding()
                && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater()));

        return true;
    }

    private void handleCrossbowSwap() {
        if (funtimeSwapped || mc.player == null) return;
        if (!mc.player.isUsingItem() || mc.player.getActiveHand() != Hand.MAIN_HAND) return;

        ItemStack active = mc.player.getActiveItem();
        if (!isConsumable(active)) return;

        if (mc.player.getOffHandStack().isOf(Items.CROSSBOW)) return;

        int crossbowSlot = InventoryUtil.findItem(Items.CROSSBOW, false);
        if (crossbowSlot == -1) return;

        funtimeCrossbowSlot = crossbowSlot;
        funtimeSwapped = true;
        InventoryUtil.swapToOffhand(crossbowSlot);
    }

    private void restoreCrossbow() {
        if (!funtimeSwapped) return;
        if (mc.player != null && funtimeCrossbowSlot != -1) {
            InventoryUtil.swapToOffhand(funtimeCrossbowSlot);
        }
        funtimeCrossbowSlot = -1;
        funtimeSwapped = false;
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.POTION
                || item == Items.MILK_BUCKET
                || item == Items.HONEY_BOTTLE
                || item == Items.BREAD
                || item == Items.COOKED_BEEF
                || item == Items.COOKED_CHICKEN
                || item == Items.COOKED_MUTTON
                || item == Items.COOKED_PORKCHOP
                || item == Items.COOKED_RABBIT
                || item == Items.CARROT
                || item == Items.GOLDEN_CARROT
                || item == Items.APPLE
                || item == Items.GOLDEN_APPLE
                || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.SWEET_BERRIES
                || item == Items.GLOW_BERRIES
                || item == Items.MELON_SLICE
                || item == Items.PUMPKIN_PIE
                || item == Items.COOKIE
                || item == Items.CAKE
                || item == Items.SUSPICIOUS_STEW
                || item == Items.RABBIT_STEW
                || item == Items.MUSHROOM_STEW
                || item == Items.BEETROOT_SOUP
                || item == Items.ENDER_PEARL
                || item == Items.BOW
                || item == Items.CROSSBOW
                || item == Items.SPYGLASS;
    }
}
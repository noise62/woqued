package sweetie.evaware.api.utils.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.other.SlownessManager;

public class ShieldBreakManager implements QuickImports {
    public boolean shouldBreakShield(PlayerEntity entity) {
        if (entity.isBlocking() && isLookingAtMe(entity)) {
            boolean slowness = SlownessManager.isEnabled();
            int invSlot = InventoryUtil.findAxeInInventory(false);
            int hotBarSlot = InventoryUtil.findAxeInInventory(true);

            if (hotBarSlot == -1 && invSlot != -1) {
                if (!slowness) {
                    return shieldBreakAction("Inventory", hotBarSlot, invSlot, entity);
                } else {
                    SlownessManager.applySlowness(10, () -> {
                        shieldBreakAction("Inventory", hotBarSlot, invSlot, entity);
                    });
                    return true;
                }
            }

            if (hotBarSlot != -1) {
                if (!slowness) {
                    shieldBreakAction("Hotbar", hotBarSlot, invSlot, entity);
                } else {
                    SlownessManager.applySlowness(150, () -> {
                        shieldBreakAction("Hotbar", hotBarSlot, invSlot, entity);
                    });
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shieldBreakAction(String action, int hotBarSlot, int invSlot, PlayerEntity entity) {
        int prevSlot = mc.player.getInventory().selectedSlot;

        switch (action) {
            case "Hotbar" -> {
                InventoryUtil.swapToSlot(hotBarSlot);
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                InventoryUtil.swapToSlot(prevSlot);
                return true;
            }

            case "Inventory" -> {
                int bestSlot = InventoryUtil.findBestSlotInHotBar();
                InventoryUtil.swapSlots(invSlot, bestSlot);
                InventoryUtil.swapToSlot(bestSlot);

                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);

                InventoryUtil.swapSlots(invSlot, bestSlot);
                InventoryUtil.swapToSlot(prevSlot);
                return true;
            }
        }

        return false;
    }

    private boolean isLookingAtMe(PlayerEntity target) {
        if (mc.player == null) return false;

        Vec3d lookVec = target.getRotationVec(1.0F).normalize(); // куда смотрит цель
        Vec3d dirToMe = mc.player.getEyePos().subtract(target.getEyePos()).normalize(); // вектор от цели к нам

        double dot = lookVec.dotProduct(dirToMe);
        double angle = Math.toDegrees(Math.acos(dot));

        return angle < 100.0; // щит ломаем только если цель реально смотрит в нашу сторону
    }
}
package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.util.hit.BlockHitResult;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Tool", category = Category.PLAYER)
public class AutoToolModule extends Module {
    @Getter private static final AutoToolModule instance = new AutoToolModule();

    private int lastSlot = -1;

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.isCreative()) {
                lastSlot = -1;
                return;
            }

            boolean breaking = mc.interactionManager.isBreakingBlock();

            if (breaking && lastSlot == -1) lastSlot = mc.player.getInventory().selectedSlot;

            mc.player.getInventory().selectedSlot = breaking
                    ? getBestToolSlot()
                    : lastSlot != -1 ? lastSlot : mc.player.getInventory().selectedSlot;

            if (!breaking) lastSlot = -1;
        }));

        addEvents(updateEvent);
    }

    private int getBestToolSlot() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return mc.player.getInventory().selectedSlot;
        Block block = mc.world.getBlockState(hit.getBlockPos()).getBlock();
        int bestSlot = mc.player.getInventory().selectedSlot;
        float bestSpeed = 1.0f;
        for (int i = 0; i < 9; i++) {
            float speed = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(block.getDefaultState());
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    @Override
    public void onDisable() {
        lastSlot = -1;
    }
}

package sweetie.evaware.client.features.modules.movement.noslow.modes;

import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import sweetie.evaware.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowSlotUpdate extends NoSlowMode {
    @Override
    public String getName() {
        return "Slot update";
    }

    @Override
    public void onUpdate() {
        sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean slowingCancel() {
        return true;
    }
}

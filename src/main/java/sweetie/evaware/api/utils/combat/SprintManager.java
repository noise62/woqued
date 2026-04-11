package sweetie.evaware.api.utils.combat;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import sweetie.evaware.api.event.events.player.move.SprintEvent;
import sweetie.evaware.api.system.interfaces.QuickImports;

public class SprintManager implements QuickImports {
    public SprintType sprintType;

    public SprintManager(SprintType sprintType) {
        this.sprintType = sprintType;
    }

    public void legitSprint(SprintEvent.SprintEventData event, boolean rule) {
        if (sprintType == SprintType.LEGIT && rule) {
            event.setSprint(false);
        }

    }

    public void packetSprint(boolean enable) {
        if (sprintType == SprintType.PACKET && mc.player.lastSprinting && !mc.player.isTouchingWater()) {
            sprintPacketState(enable);
        }
    }

    private void sprintPacketState(boolean enable) {
        sendPacket(new ClientCommandC2SPacket(mc.player, enable ? ClientCommandC2SPacket.Mode.START_SPRINTING : ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        mc.player.setSprinting(enable);
        mc.options.sprintKey.setPressed(enable);
    }

    public enum SprintType {
        LEGIT, PACKET, NONE
    }
}

package sweetie.evaware.api.utils.other;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import sweetie.evaware.api.system.backend.SharedClass;

@UtilityClass
@Accessors(fluent = true)
public class NetworkUtil {
    @Getter
    private boolean silentPacket = false;
    
    public void sendPacket(Packet<?> packet) {
        assert player() != null;

        player().networkHandler.sendPacket(packet);
    }

    public void sendPacket(SequencedPacketCreator packet) {
        assert player() != null;

        try (var ignored = player().clientWorld.pendingUpdateManager.incrementSequence()) {
            int sequence = player().clientWorld.pendingUpdateManager.getSequence();
            player().networkHandler.sendPacket(packet.predict(sequence));
        }
    }

    public void sendSilentPacket(Packet<?> packet) {
        try {
            silentPacket = true;
            sendPacket(packet);
        } finally {
            silentPacket = false;
        }
    }

    public void sendSilentPacket(SequencedPacketCreator packet) {
        try {
            silentPacket = true;
            sendPacket(packet);
        } finally {
            silentPacket = false;
        }
    }

    private ClientPlayerEntity player() {
        return SharedClass.player();
    }
}
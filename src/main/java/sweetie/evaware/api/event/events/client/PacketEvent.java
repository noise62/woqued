package sweetie.evaware.api.event.events.client;

import lombok.Getter;
import net.minecraft.network.packet.Packet;
import sweetie.evaware.api.event.events.Event;

public class PacketEvent extends Event<PacketEvent.PacketEventData> {
    @Getter private static final PacketEvent instance = new PacketEvent();

    public record PacketEventData(Packet<?> packet, PacketType packetType) {
        public boolean isReceive() {
            return packetType == PacketType.RECEIVE;
        }

        public boolean isSend() {
            return packetType == PacketType.SEND;
        }

        public enum PacketType {
            SEND, RECEIVE
        }
    }
}

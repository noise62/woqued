package sweetie.evaware.api.system.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import sweetie.evaware.api.utils.other.NetworkUtil;
import sweetie.evaware.api.utils.other.TextUtil;

public interface QuickImports {
    MinecraftClient mc = MinecraftClient.getInstance();

    default void print(String message) {
        TextUtil.sendMessage(message);
    }

    default void sendPacket(SequencedPacketCreator packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(SequencedPacketCreator packet) {
        NetworkUtil.sendSilentPacket(packet);
    }
    default void sendPacket(Packet<?> packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(Packet<?> packet) {
        NetworkUtil.sendSilentPacket(packet);
    }
}

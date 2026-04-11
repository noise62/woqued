package sweetie.evaware.inject.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.utils.other.NetworkUtil;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
    private void sendPackets(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo callbackInfo) {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null ||
                NetworkUtil.silentPacket()) return;

        if (PacketEvent.getInstance().call(new PacketEvent.PacketEventData(packet, PacketEvent.PacketEventData.PacketType.SEND))) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void receivePackets(Packet<?> packet, PacketListener listener, CallbackInfo callbackInfo) {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null) return;

        if (packet instanceof BundleS2CPacket bundlePacket) {
            for (Packet<?> innerPacket : bundlePacket.getPackets()) {
                if (handleSinglePacket(innerPacket)) {
                    callbackInfo.cancel();
                    return;
                }
            }
        } else {
            if (handleSinglePacket(packet)) {
                callbackInfo.cancel();
            }
        }
    }

    @Unique
    private static boolean handleSinglePacket(Packet<?> packet) {
        return PacketEvent.getInstance().call(new PacketEvent.PacketEventData(packet, PacketEvent.PacketEventData.PacketType.RECEIVE));
    }
}

package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

@ModuleRegister(name = "No Server Pack", category = Category.OTHER)
public class NoServerPackModule extends Module {
    @Getter private static final NoServerPackModule instance = new NoServerPackModule();

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isReceive() && event.packet() instanceof ResourcePackSendS2CPacket packet) {
                sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
                sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                PacketEvent.getInstance().setCancel(true);
            }
        }));

        addEvents(packetEvent);
    }
}

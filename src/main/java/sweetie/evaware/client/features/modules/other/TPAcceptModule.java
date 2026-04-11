package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.system.configs.FriendManager;

@ModuleRegister(name = "TP Accept", category = Category.OTHER)
public class TPAcceptModule extends Module {
    @Getter private static final TPAcceptModule instance = new TPAcceptModule();

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isReceive() && event.packet() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();

                if (message.contains("телепортироваться") || message.contains("tpaccept")) {
                    for (String name : FriendManager.getInstance().getData()) {
                        if (message.toLowerCase().contains(name.toLowerCase())) {
                            mc.player.networkHandler.sendChatCommand("tpaccept " + name);
                            break;
                        }
                    }
                }
            }
        }));

        addEvents(packetEvent);
    }
}

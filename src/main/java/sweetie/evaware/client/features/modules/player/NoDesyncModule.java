package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.utils.player.InventoryUtil;

@ModuleRegister(name = "No Desync", category = Category.PLAYER)
public class NoDesyncModule extends Module {
    @Getter private static final NoDesyncModule instance = new NoDesyncModule();

    private final BooleanSetting noRotate = new BooleanSetting("No rotate").value(true);

    public NoDesyncModule() {
        addSettings(noRotate);
    }

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            handleItemSwapFix(event);
            handleNoRotate(event);
        }));

        addEvents(packetEvent);
    }

    private void handleItemSwapFix(PacketEvent.PacketEventData event) {
        if (event.packet() instanceof UpdateSelectedSlotS2CPacket packet && event.isReceive()) {
            PacketEvent.getInstance().setCancel(true);
            InventoryUtil.swapToSlot(mc.player.getInventory().selectedSlot);
        }
    }

    private void handleNoRotate(PacketEvent.PacketEventData event) {
        if (noRotate.getValue() && event.packet() instanceof PlayerPositionLookS2CPacket packet && event.isReceive()) {
            packet.change().pitch = mc.player.getPitch();
            packet.change().yaw = mc.player.getYaw();
        }
    }
}

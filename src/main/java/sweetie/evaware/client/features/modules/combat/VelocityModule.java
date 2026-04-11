package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.player.other.MovementInputEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;

@ModuleRegister(name = "Velocity", category = Category.COMBAT)
public class VelocityModule extends Module {
    @Getter private static final VelocityModule instance = new VelocityModule();

    private final ModeSetting knockback = new ModeSetting("Knockback").value("None").values("None", "Cancel", "Jump reset");

    private final MultiBooleanSetting pushing = new MultiBooleanSetting("Pushing").value(
            new BooleanSetting("Block").value(true),
            new BooleanSetting("Liquids").value(true),
            new BooleanSetting("Entity").value(true),
            new BooleanSetting("Fishing rod").value(true)
    );

    private boolean isFallDamage = false;
    private int limitUntilJump = 0;

    public VelocityModule() {
        addSettings(knockback, pushing);
    }

    @Override
    public void onEvent() {
        PacketEvent packetInstance = PacketEvent.getInstance();
        EventListener packetEvent = packetInstance.subscribe(new Listener<>(event -> {
            handlePacketEvent(packetInstance, event);
        }));

        EventListener moveInputEvent = MovementInputEvent.getInstance().subscribe(new Listener<>(event -> {
            handleMoveInputEvent(event);
        }));

        addEvents(packetEvent, moveInputEvent);
    }

    private void handleMoveInputEvent(MovementInputEvent.MovementInputEventData event) {
        switch (knockback.getValue()) {
            case "Jump reset" -> {
                if (mc.player.hurtTime != 9 || !mc.player.isOnGround() || !mc.player.isSprinting() || isFallDamage) {
                    return;
                }

                event.setJump(true);
            }
        }
    }

    private void handlePacketEvent(PacketEvent event, PacketEvent.PacketEventData data) {
        if (data.packet() instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == mc.player.getId()) {
            switch (knockback.getValue()) {
                case "Cancel" -> {
                    event.setCancel(true);
                }

                case "Jump reset" -> {
                    isFallDamage = velocityPacket.getVelocityX() == 0.0
                            && velocityPacket.getVelocityY() == 0.0
                            && velocityPacket.getVelocityZ() < 0;
                }
            }
        }
    }

    public boolean cancelPush(PushingSource data) {
        return isEnabled() && switch (data) {
            case BLOCK -> pushing.isEnabled("Block");
            case LIQUIDS -> pushing.isEnabled("Liquids");
            case ENTITY -> pushing.isEnabled("Entity");
            case FISHING_ROD -> pushing.isEnabled("Fishing rod");
        };
    }

    public enum PushingSource {
        BLOCK, LIQUIDS, ENTITY, FISHING_ROD
    }
}

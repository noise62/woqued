package sweetie.evaware.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.player.move.CollisionEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleRegister(name = "No Clip", category = Category.MOVEMENT)
public class NoClipModule extends Module {
    @Getter private static final NoClipModule instance = new NoClipModule();

    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();

    private boolean semiPacketSent;
    private boolean skipReleaseOnDisable;

    @Override
    public void onEnable() {
        packets.clear();
        semiPacketSent = false;
        skipReleaseOnDisable = false;
    }

    @Override
    public void onDisable() {
        if (!skipReleaseOnDisable && semiPacketSent) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x - 5000, y, z - 5000, yaw, pitch, false,false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, mc.player.isOnGround(),false));
        }

        if (mc.player != null && mc.player.networkHandler != null && !packets.isEmpty()) {
            for (Packet<?> packet : packets) {
                sendSilentPacket(packet);
            }
            packets.clear();
        }
    }

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isSend()) {
                Packet<?> p = event.packet();

                if (p instanceof PlayerMoveC2SPacket) {
                    packets.add(p);
                    PacketEvent.getInstance().setCancel(true);
                }
            }
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);

            Box box = mc.player.getBoundingBox().expand(0.001D);

            int minX = MathHelper.floor(box.minX);
            int minY = MathHelper.floor(box.minY);
            int minZ = MathHelper.floor(box.minZ);
            int maxX = MathHelper.floor(box.maxX);
            int maxY = MathHelper.floor(box.maxY);
            int maxZ = MathHelper.floor(box.maxZ);

            long totalStates = 0;
            long solidStates = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = mc.world.getBlockState(pos);

                        totalStates++;
                        if (state.isSolid()) {
                            solidStates++;
                        }
                    }
                }
            }

            boolean noSolidInAABB = solidStates == 0;
            boolean semiInsideBlock = solidStates > 0 && solidStates < totalStates;

            if (!semiPacketSent && semiInsideBlock) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                float yaw = mc.player.getYaw();
                float pitch = mc.player.getPitch();
                boolean onGround = mc.player.isOnGround();

                for (int i = 0; i < 2; i++) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, false));
                }
                semiPacketSent = true;
                return;
            }

            if (semiPacketSent && noSolidInAABB) {
                skipReleaseOnDisable = true;
                toggle();
            }
        }));

        EventListener collisionEvent = CollisionEvent.getInstance().subscribe(new Listener<>(event -> {
            CollisionEvent.getInstance().setCancel(true);
        }));
        
        addEvents(packetEvent, updateEvent, collisionEvent);
    }
}

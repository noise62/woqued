package worst.woqued.client.features.modules.player;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.PacketEvent;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.color.ColorUtil;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.math.TimerUtil;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.display.BoxRender;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleRegister(name = "Fake Lag", category = Category.PLAYER)
public class FakeLagModule extends Module {
    @Getter private static final FakeLagModule instance = new FakeLagModule();

    private final SliderSetting resetTime = new SliderSetting("Reset time").value(100f).range(0f, 1000f).step(10f);
    private final BooleanSetting render = new BooleanSetting("Render").value(true);
    private final BooleanSetting resetOnKnockback = new BooleanSetting("Reset on knockback").value(true);

    private Vec3d lastPos = Vec3d.ZERO;
    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean isCancel;

    public FakeLagModule() {
        addSettings(resetTime, render, resetOnKnockback);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!cancelWork() && timerUtil.finished(resetTime.getValue().longValue())) {
                resetFakeLag();
            }
        }));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (cancelWork()) return;
            if (event.isReceive()) {
                if (event.packet() instanceof ExplosionS2CPacket) {
                    resetFakeLag();
                }
                if (event.packet()  instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
                    if (velocityPacket.getEntityId() == mc.player.getId() && resetOnKnockback.getValue()) {
                        resetFakeLag();
                    }
                }
            } else if (event.isSend()) {
                if (event.packet()  instanceof PlayerInteractEntityC2SPacket || event.packet()  instanceof UpdateSelectedSlotC2SPacket || event.packet()  instanceof HandSwingC2SPacket || event.packet()  instanceof PlayerInteractBlockC2SPacket || event.packet()  instanceof PlayerInteractItemC2SPacket || event.packet()  instanceof ClickSlotC2SPacket) {
                    resetFakeLag();
                    return;
                }
                if (!isCancel) {
                    packets.add(event.packet());
                    PacketEvent.getInstance().setCancel(true);
                }
            }
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (cancelWork()) return;
            if (render.getValue()) {
                Entity player = mc.player;
                float x = (float) lastPos.x;
                float y = (float) lastPos.y + 1f;
                float z = (float) lastPos.z;
                RenderUtil.BOX.drawBox(x, y, z, x + player.getWidth(), y + player.getHeight(), z + player.getWidth(), 3, ColorUtil.setAlpha(UIColors.gradient(0), 200), BoxRender.Render.STRIPED, player.getWidth() / 5.5f);
            }
        }));

        addEvents(tickEvent, packetEvent, renderEvent);
    }

    private boolean cancelWork() {
        return mc.isInSingleplayer();
    }

    private void resetFakeLag() {
        isCancel = true;
        while (!packets.isEmpty()) {
            sendPacket(packets.poll());
        }
        isCancel = false;
        timerUtil.reset();
        if (mc.player == null) return;
        lastPos = mc.player.getPos();
    }

    @Override
    public void onEnable() {
        timerUtil.reset();
        if (mc.player == null) return;
        lastPos = mc.player.getPos();
    }

    @Override
    public void onDisable() {
        resetFakeLag();
    }
}

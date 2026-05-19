package worst.woqued.client.features.modules.render;

import com.mojang.authlib.GameProfile;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.PacketEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import lombok.Getter;

@ModuleRegister(name = "Free Cam", category = Category.RENDER)
public class FreeCamModule extends Module {
    @Getter
    private static final FreeCamModule instance = new FreeCamModule();

    private static final int FAKE_PLAYER_ID = -92143;

    private Vec3d startPos = Vec3d.ZERO;
    private float startYaw;
    private float startPitch;
    private boolean startNoClip;
    private boolean startFlying;
    private float startFlySpeed;
    private OtherClientPlayerEntity fakePlayer;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        startPos = mc.player.getPos();
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
        startNoClip = mc.player.noClip;
        startFlying = mc.player.getAbilities().flying;
        startFlySpeed = mc.player.getAbilities().getFlySpeed();

        spawnClone();
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        mc.player.noClip = startNoClip;
        mc.player.getAbilities().flying = startFlying;
        mc.player.getAbilities().setFlySpeed(startFlySpeed);
        mc.player.setVelocity(Vec3d.ZERO);
        mc.player.setPosition(startPos);
        mc.player.setYaw(startYaw);
        mc.player.setPitch(startPitch);
        mc.player.setHeadYaw(startYaw);
        mc.player.setBodyYaw(startYaw);

        removeClone();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) {
                return;
            }

            mc.player.noClip = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlySpeed(0.08f);
            mc.player.setOnGround(false);
            mc.player.setVelocity(getFreecamVelocity());
        }));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isSend() && event.packet() instanceof PlayerMoveC2SPacket) {
                PacketEvent.getInstance().setCancel(true);
            }
        }));

        addEvents(updateEvent, packetEvent);
    }

    private Vec3d getFreecamVelocity() {
        if (mc.player == null) {
            return Vec3d.ZERO;
        }

        float speed = mc.options.sprintKey.isPressed() ? 0.62f : 0.34f;
        float yawRad = mc.player.getYaw() * 0.017453292f;
        float pitchRad = mc.player.getPitch() * 0.017453292f;

        float forward = 0f;
        float strafe = 0f;
        if (mc.options.forwardKey.isPressed()) forward += 1f;
        if (mc.options.backKey.isPressed()) forward -= 1f;
        if (mc.options.leftKey.isPressed()) strafe += 1f;
        if (mc.options.rightKey.isPressed()) strafe -= 1f;

        float vertical = 0f;
        if (mc.options.jumpKey.isPressed()) vertical += 1f;
        if (mc.options.sneakKey.isPressed()) vertical -= 1f;

        if (forward != 0f && strafe != 0f) {
            forward *= 0.70710677f;
            strafe *= 0.70710677f;
        }

        double motionX = (-MathHelper.sin(yawRad) * forward + MathHelper.cos(yawRad) * strafe) * speed;
        double motionZ = (MathHelper.cos(yawRad) * forward + MathHelper.sin(yawRad) * strafe) * speed;
        double motionY = vertical * speed;

        if (forward != 0f && vertical != 0f) {
            motionY += -Math.sin(pitchRad) * forward * speed * 0.2f;
        }

        return new Vec3d(motionX, motionY, motionZ);
    }

    private void spawnClone() {
        removeClone();
        if (mc.world == null || mc.player == null) {
            return;
        }

        GameProfile profile = mc.player.getGameProfile();
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHeadYaw(mc.player.getHeadYaw());
        fakePlayer.setBodyYaw(mc.player.bodyYaw);
        fakePlayer.getInventory().clone(mc.player.getInventory());
        mc.world.addEntity(fakePlayer);
    }

    private void removeClone() {
        if (mc.world != null && fakePlayer != null) {
            fakePlayer.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
        }
        fakePlayer = null;
    }
}

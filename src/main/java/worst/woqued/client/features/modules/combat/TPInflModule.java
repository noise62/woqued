package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.SliderSetting;


import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Tp Aura", category = Category.COMBAT)
public class TPInflModule extends Module {
    @Getter private static final TPInflModule instance = new TPInflModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("BlockTPRuleVH").values("BlockTPRuleVH", "StepVH");
    public final SliderSetting range = new SliderSetting("Range").value(15f).range(10f, 200f).step(1f);

    private PlayerEntity currentTarget = null;
    private long lastTpTime = 0L;
    private Vec3d lastHandledVec = new Vec3d(0.0D, 0.0D, 0.0D);
    private final List<TimedRunnable> postDoingRuns = new ArrayList<>();

    public TPInflModule() {
        addSettings(mode, range);
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        lastTpTime = 0L;
        lastHandledVec = new Vec3d(0, 0, 0);
        postDoingRuns.clear();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            onTick();
        }));
        addEvents(tickEvent);
    }

    private void onTick() {
        updatePostActionsRuns(false);
        if (mc.player == null || mc.world == null) return;
        PlayerEntity newTarget = findTarget();
        if (newTarget != null && newTarget != currentTarget) {
            currentTarget = newTarget;
            performTeleport();
        }
    }

    private void performTeleport() {
        if (currentTarget == null || mc.player == null) return;

        double distance = mc.player.distanceTo(currentTarget);
        if (distance > range.getValue()) return;

        Vec3d targetPos = calculateTargetPosition(currentTarget);
        executeTeleport(mc.player.getPos(), targetPos);
    }

    private void executeTeleport(Vec3d from, Vec3d to) {
        double dx = Math.abs(from.x - to.x);
        double dy = Math.abs(from.y - to.y);
        double dz = Math.abs(from.z - to.z);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float distanceDensity = 1.0F;
        int packetCount = (int)(distance / (9.64D * distanceDensity)) + 1;
        for(int i = 0; i < packetCount; ++i) {
            sendGroundPacket(false);
        }
        sendPositionPacket(to.x, to.y, to.z, false);
        mc.player.setPosition(to);
        lastHandledVec = to;
    }

    private Vec3d calculateTargetPosition(PlayerEntity target) {
        Vec3d targetPos = target.getPos();
        Vec3d direction = targetPos.subtract(mc.player.getPos()).normalize();
        double offsetDistance = 2.0D;
        Vec3d teleportPos = targetPos.subtract(direction.multiply(offsetDistance));
        BlockPos groundPos = findNearestSolidBlock(teleportPos, 5);
        if (groundPos != null) {
            teleportPos = new Vec3d(teleportPos.x, groundPos.getY() + 0.15, teleportPos.z);
        }

        return teleportPos;
    }

    private BlockPos findNearestSolidBlock(Vec3d pos, double radius) {
        BlockPos center = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);

        for (int y = 0; y < radius; y++) {
            BlockPos checkPos = center.down(y);
            if (mc.world.getBlockState(checkPos).isSolid()) {
                return checkPos;
            }
        }

        return null;
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity closest = null;
        float closestDist = Float.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;

            float dist = mc.player.distanceTo(player);
            if (dist <= range.getValue() && dist < closestDist) {
                closest = player;
                closestDist = dist;
            }
        }

        return closest;
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, false)
        );
    }

    private void sendGroundPacket(boolean onGround) {
        mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.OnGroundOnly(onGround, false)
        );
    }

    private void updatePostActionsRuns(boolean clear) {
        if (clear) {
            postDoingRuns.clear();
        } else if (!postDoingRuns.isEmpty()) {
            postDoingRuns.removeIf(TimedRunnable::doIfRemove);
        }
    }

    private void teleportActionDoingPost(Runnable run, int ticksAfter) {
        postDoingRuns.add(new TimedRunnable(run, ticksAfter * 50.0F - 5.0F));
    }

    @Override
    public void onDisable() {
        updatePostActionsRuns(true);
        lastTpTime = 0L;
        currentTarget = null;
    }

    private static class TimedRunnable {
        private final long startTime = System.currentTimeMillis();
        private final float maxTime;
        private Runnable runnable;

        public TimedRunnable(Runnable runnable, float maxTime) {
            this.runnable = runnable;
            this.maxTime = maxTime;
        }

        private float getTimePC() {
            return MathHelper.clamp((float)(System.currentTimeMillis() - startTime) / maxTime, 0.0F, 1.0F);
        }

        public boolean doIfRemove() {
            if (getTimePC() >= 1.0F && runnable != null) {
                runnable.run();
                runnable = null;
                return true;
            } else {
                return runnable == null;
            }
        }
    }
}
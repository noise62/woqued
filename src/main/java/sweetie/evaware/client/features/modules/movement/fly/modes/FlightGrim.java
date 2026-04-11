package sweetie.evaware.client.features.modules.movement.fly.modes;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.events.player.move.MotionEvent;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.client.TimerManager;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.player.MoveUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;
import sweetie.evaware.api.utils.rotation.manager.RotationStrategy;
import sweetie.evaware.api.utils.rotation.rotations.SnapRotation;
import sweetie.evaware.api.utils.task.TaskPriority;
import sweetie.evaware.client.features.modules.movement.fly.FlightMode;
import sweetie.evaware.client.features.modules.movement.fly.FlightModule;

import java.util.function.Supplier;

public class FlightGrim extends FlightMode {
    @Override
    public String getName() {
        return "Grim";
    }

    public BypassType bypassType;
    private final FlightModule module;

    private TimerUtil ticks = new TimerUtil();
    private long speedRampStartTime = 0;
    private boolean isSpeedRamping = false;

    @Getter private final ModeSetting grimType = new ModeSetting("Grim mode").value(BypassType.VERTICAL_ELYTRA)
            .values(BypassType.values())
            .onAction(() -> {
                bypassType = switch (getGrimType().getValue()) {
                    case "Glide elytra" -> BypassType.GLIDE_ELYTRA;
                    default -> BypassType.VERTICAL_ELYTRA;
                };
            });

    public FlightGrim(Supplier<Boolean> condition, FlightModule module) {
        grimType.setVisible(condition);
        this.module = module;
        addSettings(grimType);
    }

    @Override
    public void onUpdate() {
        if (bypassType == BypassType.GLIDE_ELYTRA) return;
        if (mc.player.isGliding() && (mc.player.getVelocity().y > 0.08 || mc.player.fallDistance > 0.1f) && (mc.player.getVelocity().x <= 0.01 && mc.player.getVelocity().z <= 0.01)) {
            RotationStrategy rotationStrategy = new RotationStrategy(new SnapRotation(), true);

            mc.player.getVelocity().z = 0.0;
            mc.player.getVelocity().x = 0.0;

            RotationManager rotationManager = RotationManager.getInstance();
            Rotation rotation = rotationManager.getRotation();
            RotationPlan configurable = rotationManager.getCurrentRotationPlan();
            float pitch = configurable != null ? rotation.getPitch() : mc.player.getPitch();

            boolean validPitch = mc.player.getPitch() >= -30.0f && mc.player.getPitch() <= 30.0f;

            if (!isSpeedRamping) {
                speedRampStartTime = System.currentTimeMillis();
                isSpeedRamping = true;
            }

            long rampDuration = 100L;
            long elapsed = System.currentTimeMillis() - speedRampStartTime;
            float progress = Math.min(elapsed / (float)rampDuration, 1f);
            double currentBaseSpeed = (0.05 * progress);

            double maxAddedSpeed = 0.06;
            double maxVerticalSpeed = 1.11;

            float normalizedPitch = pitch / 90f;
            double speedAddition = maxAddedSpeed * normalizedPitch * normalizedPitch;

            double superKuniMan = currentBaseSpeed + speedAddition;
            mc.player.getVelocity().y += superKuniMan;

            if (mc.player.getVelocity().y >= maxVerticalSpeed) {
                mc.player.getVelocity().y = maxVerticalSpeed;
            }

            if (!validPitch) {
                RotationManager.getInstance().addRotation(new Rotation(mc.player.getYaw(), 0f), rotationStrategy, TaskPriority.NORMAL, module);
            }
        } else {
            isSpeedRamping = false;
        }
    }


    /**
     * Что бы я не сделала, что бы не увидела
     * Всё, к чему коснусь — становится невидимым
     * Всё, на что смотрю — сразу испаряется
     * Дружба и любовь — со мною не случаются
     */
    @Override
    public void onMotion(MotionEvent.MotionEventData event) {
        if (bypassType == BypassType.VERTICAL_ELYTRA || !mc.player.isGliding()) return;
        Vec3d pos = mc.player.getPos();

        float yaw = mc.player.getYaw();
        double forward = 6.087;
        double motion = MathUtil.getEntityBPS(mc.player);

        float doni = mc.getNetworkHandler().getServerInfo() != null &&  mc.getNetworkHandler().getServerInfo().address.contains("reallyworld") ? 48f : 52f;
        if (motion >= doni) {
            forward = 0f;
            motion = 0;
        }

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;
        mc.player.setVelocity(dx * MathUtil.randomInRange(1.1f, 1.21f), mc.player.getVelocity().y - 0.02f, dz * MathUtil.randomInRange(1.1f, 1.21f));

        if (ticks.finished(50)) {
            mc.player.setPosition(pos.x + dx, pos.y, pos.z + dz);
            ticks.reset();
        }
        mc.player.setVelocity(dx * MathUtil.randomInRange(1.1f, 1.21f), mc.player.getVelocity().y + 0.016f, dz * MathUtil.randomInRange(1.1f, 1.21f));
    }

    public enum BypassType implements ModeSetting.NamedChoice {
        VERTICAL_ELYTRA("Vertical elytra"),
        GLIDE_ELYTRA("Glide elytra");

        private final String name;

        BypassType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}

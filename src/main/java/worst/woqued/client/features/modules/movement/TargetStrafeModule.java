package worst.woqued.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationStrategy;
import worst.woqued.api.utils.task.TaskPriority;
import worst.woqued.client.features.modules.combat.AuraModule;

@ModuleRegister(name = "TargetStrafe", category = Category.MOVEMENT, bind = -999)
public class TargetStrafeModule extends Module {
    @Getter private static final TargetStrafeModule instance = new TargetStrafeModule();

    // Settings
    private final BooleanSetting jump = new BooleanSetting("Jump").value(true);
    private final SliderSetting distance = new SliderSetting("Distance").value(2.5f).range(0.5f, 6.0f);
    private final SliderSetting speed = new SliderSetting("Speed").value(1.8f).range(0.5f, 5.0f);
    private final BooleanSetting aggressive = new BooleanSetting("Aggressive").value(true);
    private final SliderSetting strafeRadius = new SliderSetting("StrafeRadius").value(1.5f).range(0.5f, 4.0f);

    private boolean switchDir = false;
    private int strafeTimer = 0;

    public TargetStrafeModule() {
        addSettings(jump, distance, speed, aggressive, strafeRadius);
    }

    @Override
    public void onEnable() {
        switchDir = false;
        strafeTimer = 0;
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.player.input != null) {
            mc.player.input.movementForward = 0.0f;
            mc.player.input.movementSideways = 0.0f;
        }
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            onTick();
        }));
        addEvents(tickEvent);
    }

    private boolean canStrafe() {
        if (mc.player == null || mc.world == null) return false;
        return !mc.player.getAbilities().flying;
    }

    private void onTick() {
        if (!canStrafe()) return;

        AuraModule aura = AuraModule.getInstance();

        if (aura.target != null && aura.isEnabled()) {
            LivingEntity target = aura.target;

            if (mc.player.isOnGround() && jump.getValue()) {
                mc.player.jump();
            }

            double currentDistance = Math.sqrt(mc.player.squaredDistanceTo(target));
            double moveSpeed = speed.getValue();

            if (currentDistance > distance.getValue()) {
                double deltaX = target.getX() - mc.player.getX();
                double deltaZ = target.getZ() - mc.player.getZ();
                double targetDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                if (targetDistance > 0) {
                    double normalizedX = deltaX / targetDistance;
                    double normalizedZ = deltaZ / targetDistance;

                    double finalSpeed = aggressive.getValue() ? moveSpeed * 1.2 : moveSpeed;

                    mc.player.setVelocity(
                            normalizedX * finalSpeed * 0.3,
                            mc.player.getVelocity().y,
                            normalizedZ * finalSpeed * 0.3
                    );

                    mc.player.input.movementForward = 1.0f;
                    mc.player.input.movementSideways = 0.0f;

                    double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
                    RotationManager.getInstance().addRotation(
                            new Rotation((float) yaw, mc.player.getPitch()),
                            RotationStrategy.TARGET,
                            TaskPriority.LOWEST,
                            this
                    );
                }
            } else {
                strafeTimer++;

                if (strafeTimer > 40) {
                    switchDir = !switchDir;
                    strafeTimer = 0;
                }

                double angle = Math.atan2(mc.player.getZ() - target.getZ(), mc.player.getX() - target.getX());
                angle += switchDir ? 0.15 : -0.15;

                double strafeX = target.getX() + strafeRadius.getValue() * Math.cos(angle);
                double strafeZ = target.getZ() + strafeRadius.getValue() * Math.sin(angle);

                double deltaX = strafeX - mc.player.getX();
                double deltaZ = strafeZ - mc.player.getZ();
                double strafeDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                if (strafeDistance > 0.1) {
                    double normalizedX = deltaX / strafeDistance;
                    double normalizedZ = deltaZ / strafeDistance;
                    double strafeSpeed = moveSpeed * 0.8;

                    mc.player.setVelocity(
                            normalizedX * strafeSpeed * 0.25,
                            mc.player.getVelocity().y,
                            normalizedZ * strafeSpeed * 0.25
                    );

                    mc.player.input.movementForward = 0.8f;
                    mc.player.input.movementSideways = switchDir ? 0.6f : -0.6f;

                    double strafeYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
                    RotationManager.getInstance().addRotation(
                            new Rotation((float) strafeYaw, mc.player.getPitch()),
                            RotationStrategy.TARGET,
                            TaskPriority.LOWEST,
                            this
                    );
                }
            }
        }
    }
}

package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.event.events.other.RotationUpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.MultiBooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.combat.TargetManager;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationStrategy;
import worst.woqued.api.utils.rotation.rotations.AimAssistRotation;
import worst.woqued.api.utils.task.TaskPriority;
import worst.woqued.api.system.interfaces.QuickImports;

@Getter
@ModuleRegister(name = "Aim Assist", category = Category.COMBAT)
public class AimAssistModule extends Module implements QuickImports {
    private static final AimAssistModule instance = new AimAssistModule();

    public static AimAssistModule getInstance() {
        return instance;
    }

    private final SliderSetting aimFov = new SliderSetting("FOV").value(180.0f).range(90.0f, 360.0f).step(1.0f);
    private final SliderSetting aimRange = new SliderSetting("Range").value(100.0f).range(10.0f, 200.0f).step(1.0f);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed").value(20.0f).range(5.0f, 60.0f).step(1.0f);
    private final ModeSetting aimPoint = new ModeSetting("Aim Point").value("Head").values("Head", "Body", "Legs");
    private final ModeSetting targetMode = new ModeSetting("Target Mode").value("FOV").values("FOV", "Distance");

    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls").value(false);
    private final BooleanSetting autoPitch = new BooleanSetting("Auto Pitch").value(true);

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(true),
            new BooleanSetting("Animals").value(false)
    );

    private TargetManager.EntityFilter entityFilter;
    private final AimAssistRotation aimAssistRotation = new AimAssistRotation();

    private Entity currentTarget = null;
    private long lastValidTargetTime = 0;

    public AimAssistModule() {
        entityFilter = new TargetManager.EntityFilter(targets.getList());
        addSettings(aimFov, aimRange, aimSpeed, aimPoint, targetMode, throughWalls, autoPitch, targets);
    }

    @Override
    public void onDisable() {
        RotationManager.getInstance().clear();
        currentTarget = null;
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            Entity target = findBestTarget();

            if (target != null) {
                currentTarget = target;
                lastValidTargetTime = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - lastValidTargetTime > 150) {
                    currentTarget = null;
                    RotationManager.getInstance().clear();
                }
            }
        }));

        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (currentTarget == null || !(currentTarget instanceof LivingEntity) || mc.player == null || mc.world == null) return;

            Vec3d targetPos = getAimPoint(currentTarget);
            Vec3d playerPos = mc.player.getEyePos();
            Vec3d delta = targetPos.subtract(playerPos);

            Rotation rotation = RotationUtil.fromVec3d(delta);

            RotationStrategy strategy = new RotationStrategy(aimAssistRotation, true, true)
                    .clientLook(true)
                    .ticksUntilReset(3);

            RotationManager.getInstance().addRotation(
                    new Rotation.VecRotation(rotation, targetPos),
                    (LivingEntity) currentTarget,
                    strategy,
                    TaskPriority.HIGH,
                    this
            );
        }));

        addEvents(tickEvent, rotationUpdateEvent);
    }

    private Entity findBestTarget() {
        if (mc.player == null || mc.world == null) return null;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        Entity bestEntity = null;
        float bestValue = targetMode.is("Distance") ? Float.MAX_VALUE : aimFov.getValue();
        double bestDistance = Double.MAX_VALUE;

        boolean prioritizeDistance = targetMode.is("Distance");

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValidTarget(entity)) continue;

            Vec3d targetPos = getAimPoint(entity);
            Vec3d delta = targetPos.subtract(playerPos);
            double distance = delta.length();

            if (distance > aimRange.getValue()) continue;
            if (!throughWalls.getValue() && !mc.player.canSee(entity)) continue;

            if (prioritizeDistance) {
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestEntity = entity;
                }
            } else {
                Vec3d deltaNorm = delta.normalize();
                double dot = lookVec.dotProduct(deltaNorm);
                double angle = Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));

                if (angle < bestValue) {
                    bestValue = (float) angle;
                    bestEntity = entity;
                }
            }
        }

        if (bestEntity != null && currentTarget != null && isValidTarget(currentTarget)) {
            Vec3d currentTargetPos = getAimPoint(currentTarget);
            double currentDistance = currentTargetPos.subtract(playerPos).length();
            if (currentDistance <= aimRange.getValue()) {
                if (prioritizeDistance) {
                    if (currentDistance < bestDistance * 1.2) {
                        return currentTarget;
                    }
                } else {
                    Vec3d currentDelta = currentTargetPos.subtract(playerPos).normalize();
                    double currentDot = lookVec.dotProduct(currentDelta);
                    double currentAngle = Math.toDegrees(Math.acos(MathHelper.clamp(currentDot, -1.0, 1.0)));
                    if (currentAngle < bestValue * 1.5) {
                        return currentTarget;
                    }
                }
            }
        }

        return bestEntity;
    }

    private Vec3d getAimPoint(Entity entity) {
        Box box = entity.getBoundingBox();
        double minY = box.minY;
        double maxY = box.maxY;
        double height = maxY - minY;
        double centerX = box.minX + (box.maxX - box.minX) * 0.5;
        double centerZ = box.minZ + (box.maxZ - box.minZ) * 0.5;

        return switch (aimPoint.getValue()) {
            case "Head" -> new Vec3d(centerX, maxY - height * 0.15, centerZ);
            case "Body" -> new Vec3d(centerX, minY + height * 0.5, centerZ);
            case "Legs" -> new Vec3d(centerX, minY + height * 0.15, centerZ);
            default -> box.getCenter();
        };
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return false;
        if (!livingEntity.isAlive()) return false;
        if (entity == mc.player) return false;

        entityFilter.targetSettings = targets.getList();
        return entityFilter.isValid(livingEntity);
    }
}
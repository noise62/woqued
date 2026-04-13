package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.combat.TargetManager;

import java.util.Random;

@ModuleRegister(name = "AimAssist", category = Category.COMBAT)
public class AimAssistModule extends Module {
    @Getter private static final AimAssistModule instance = new AimAssistModule();

    // Основные настройки
    private final SliderSetting aimFov = new SliderSetting("FOV").value(180.0f).range(90.0f, 360.0f).step(1.0f);
    private final SliderSetting aimRange = new SliderSetting("Range").value(100.0f).range(10.0f, 200.0f).step(1.0f);
    private final SliderSetting yawSpeed = new SliderSetting("Yaw Speed").value(5.0f).range(1.0f, 240.0f).step(0.1f);
    private final SliderSetting pitchSpeed = new SliderSetting("Pitch Speed").value(5.0f).range(1.0f, 240.0f).step(0.1f);
    private final ModeSetting aimPoint = new ModeSetting("Aim Point").value("Head").values("Head", "Body", "Legs");

    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls").value(false);
    private final BooleanSetting autoPitch = new BooleanSetting("Auto Pitch").value(true);

    private TargetManager.EntityFilter entityFilter;

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(true),
            new BooleanSetting("Animals").value(false)
    );

    private Entity currentTarget = null;
    private final Random random = new Random();
    private float lastYaw = 0;
    private float lastPitch = 0;
    private long lastTargetTime = 0;
    private long lastMoveTime = 0;

    public AimAssistModule() {
        entityFilter = new TargetManager.EntityFilter(targets.getList());
        addSettings(aimFov, aimRange, yawSpeed, pitchSpeed, aimPoint, throughWalls, autoPitch, targets);
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        lastYaw = 0;
        lastPitch = 0;
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            Entity target = getBestTarget();
            if (target != null) {
                applySuperSmoothAim(target);
                currentTarget = target;
                lastTargetTime = System.currentTimeMillis();
            } else {
                currentTarget = null;
            }
        }));

        addEvents(updateEvent);
    }

    private Entity getBestTarget() {
        if (mc.player == null || mc.world == null) return null;

        boolean isCritting = mc.player.fallDistance > 0.1f;
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        Entity bestEntity = null;
        float bestAngle = aimFov.getValue();

        var entities = mc.world.getEntities();

        for (Entity entity : entities) {
            if (entity == mc.player) continue;
            if (!isValidTarget(entity)) continue;

            Vec3d targetPos = getAimPoint(entity, isCritting);
            Vec3d delta = targetPos.subtract(playerPos);
            double distance = delta.length();

            if (distance > aimRange.getValue()) continue;
            if (!throughWalls.getValue() && !mc.player.canSee(entity)) continue;

            Vec3d deltaNorm = delta.normalize();
            double dot = lookVec.dotProduct(deltaNorm);
            double angle = Math.toDegrees(Math.acos(dot));

            if (angle < bestAngle) {
                bestAngle = (float) angle;
                bestEntity = entity;
            }
        }

        // Проверка текущей цели для плавности
        if (bestEntity != null && System.currentTimeMillis() - lastTargetTime < 150) {
            if (currentTarget != null && isValidTarget(currentTarget)) {
                Vec3d delta = getAimPoint(currentTarget, isCritting).subtract(playerPos);
                if (delta.length() <= aimRange.getValue()) {
                    return currentTarget;
                }
            }
        }

        return bestEntity;
    }

    private Vec3d getAimPoint(Entity entity, boolean critMode) {
        Box box = entity.getBoundingBox();
        double minY = box.minY;
        double maxY = box.maxY;
        double height = maxY - minY;
        double centerX = box.minX + (box.maxX - box.minX) * 0.5;
        double centerZ = box.minZ + (box.maxZ - box.minZ) * 0.5;

        // В режиме крита целимся ниже
        String point = critMode ? "Legs" : aimPoint.getValue();

        switch (point) {
            case "Head":
                return new Vec3d(centerX, maxY - height * 0.1, centerZ);
            case "Body":
                return new Vec3d(centerX, minY + height * 0.5, centerZ);
            case "Legs":
                return new Vec3d(centerX, minY + height * 0.15, centerZ);
            default:
                return box.getCenter();
        }
    }

    private Vec3d getAimPoint(Entity entity) {
        return getAimPoint(entity, false);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return false;
        if (!livingEntity.isAlive()) return false;
        if (entity == mc.player) return false;

        entityFilter.targetSettings = targets.getList();
        return entityFilter.isValid(livingEntity);
    }

    private void applySuperSmoothAim(Entity target) {
        if (mc.player == null) return;

        boolean isCritting = mc.player.fallDistance > 0.1f;
        Vec3d targetPos = getAimPoint(target, isCritting);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d delta = targetPos.subtract(playerPos);

        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontalDistance));

        // Если autoPitch выключен — ограничиваем угол наклона
        if (!autoPitch.getValue()) {
            targetPitch = MathHelper.clamp(targetPitch, -15.0f, 15.0f);
        } else {
            // Auto Pitch: позволяем смотреть вниз, но не выше головы
            targetPitch = MathHelper.clamp(targetPitch, -90.0f, 15.0f);
        }

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        if (Math.abs(yawDelta) > aimFov.getValue()) return;

        long now = System.currentTimeMillis();
        float deltaTime = Math.min(0.05f, (now - lastMoveTime) / 1000.0f);
        lastMoveTime = now;
        if (deltaTime <= 0.001f) deltaTime = 0.016f;

        // Скорость в градусах за секунду (FPS-based)
        float yawDegreesPerSecond = yawSpeed.getValue();
        float pitchDegreesPerSecond = pitchSpeed.getValue();

        // Сколько градусов можем повернуть за этот кадр
        float maxYawDelta = yawDegreesPerSecond * deltaTime;
        float maxPitchDelta = pitchDegreesPerSecond * deltaTime;

        // Ограничиваем изменение
        float clampedYawDelta = MathHelper.clamp(yawDelta, -maxYawDelta, maxYawDelta);
        float clampedPitchDelta = MathHelper.clamp(pitchDelta, -maxPitchDelta, maxPitchDelta);

        float newYaw = currentYaw + clampedYawDelta;
        float newPitch = currentPitch + clampedPitchDelta;

        // Микро-шум для обхода античитов
        float noiseYaw = (random.nextFloat() - 0.5f) * 0.06f;
        float noisePitch = (random.nextFloat() - 0.5f) * 0.04f;
        newYaw += noiseYaw;
        newPitch += noisePitch;

        // Polar - ограничиваем скорость вращения между кадрами
        if (lastYaw != 0) {
            float yawRate = newYaw - lastYaw;
            float maxRate = yawDegreesPerSecond * 0.15f;
            if (Math.abs(yawRate) > maxRate) {
                newYaw = lastYaw + Math.signum(yawRate) * maxRate;
            }
        }
        lastYaw = newYaw;
        lastPitch = newPitch;

        // Sloth - периодический микро-шум
        if (now % 2000 < 20) {
            newYaw += (random.nextFloat() - 0.5f) * 0.1f;
            newPitch += (random.nextFloat() - 0.5f) * 0.08f;
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }
}

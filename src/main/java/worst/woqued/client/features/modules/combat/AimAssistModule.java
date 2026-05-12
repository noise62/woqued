package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.entity.EquipmentSlot;
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
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.combat.TargetManager;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.system.interfaces.QuickImports;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@ModuleRegister(name = "AimAssist", category = Category.COMBAT)
public class AimAssistModule extends Module implements QuickImports {
    private static final AimAssistModule instance = new AimAssistModule();

    public static AimAssistModule getInstance() {
        return instance;
    }

    public Entity getCurrentTarget() {
        return FD;
    }

    private final SliderSetting distanceSetting = new SliderSetting("Distance").value(4.0f).range(1.0f, 6.0f).step(0.1f);
    private final SliderSetting fovSetting = new SliderSetting("FOV").value(90.0f).range(20.0f, 360.0f).step(1.0f);
    private final SliderSetting speedSetting = new SliderSetting("Speed").value(4.0f).range(0.5f, 50.0f).step(0.1f);
    private final SliderSetting aimHeightSetting = new SliderSetting("Aim Height").value(0.85f).range(0.0f, 1.0f).step(0.05f);
    private final ModeSetting prioritySetting = new ModeSetting("Priority").value("Distance").values("Distance", "Health");
    private final BooleanSetting onlyWithWeaponSetting = new BooleanSetting("Only Weapon").value(false);
    private final BooleanSetting onlyPlayersSetting = new BooleanSetting("Only Players").value(true);
    private final BooleanSetting noAimInInventorySetting = new BooleanSetting("No Inventory").value(true);
    private final BooleanSetting hitInvisibleSetting = new BooleanSetting("Hit Invisible").value(false);
    private final BooleanSetting onlyArmoredSetting = new BooleanSetting("Only Armored").value(false);
    private final BooleanSetting ignoreNakedSetting = new BooleanSetting("Ignore Naked").value(false);
    private final BooleanSetting onlyXSetting = new BooleanSetting("Only X").value(false);
    private final BooleanSetting multipointSetting = new BooleanSetting("Multipoint").value(true);
    private final BooleanSetting wallCheckSetting = new BooleanSetting("Wall Check").value(true);
    private final BooleanSetting microMovementsSetting = new BooleanSetting("Micro Movements").value(true);

    private Entity FD = null;
    private long Dp = 0L;
    private long aeD = 0L;
    private double aeX = 0.0;
    private double nX = 0.0;
    private double RM;
    private double ayG;
    private double SL;
    private double Rt;
    private double alh;
    private double yY;
    private double akx;
    private double Nk;
    private long aho;
    private int adW;
    private double all;
    private double ef;
    private double aox;
    private double vK;
    private double aiH;
    private double PK;
    private long KJ;

    public AimAssistModule() {
        ignoreNakedSetting.setVisible(() -> !onlyArmoredSetting.getValue());
        addSettings(distanceSetting, fovSetting, speedSetting, aimHeightSetting, prioritySetting,
                onlyWithWeaponSetting, onlyPlayersSetting, noAimInInventorySetting, hitInvisibleSetting,
                onlyArmoredSetting, ignoreNakedSetting, onlyXSetting, multipointSetting,
                wallCheckSetting, microMovementsSetting);
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            resetState();
            Entity target = findBestTarget();

            if (target != null) {
                if (target != FD) {
                    FD = target;
                    Dp = System.currentTimeMillis();
                    randomizeParams();
                }
                updateMicroMovements();
                updateMultipoint();
            }
        }));

        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (FD == null || !(FD instanceof LivingEntity) || mc.player == null || mc.world == null) return;

            if (!onlyWithWeaponSetting.getValue() || hasWeapon()) {
                if (!noAimInInventorySetting.getValue() || mc.currentScreen == null) {
                    applyAimAssist();
                }
            }
        }));

        addEvents(tickEvent, rotationUpdateEvent);
    }

    private void resetState() {
        FD = null;
        aeX = 0.0;
        nX = 0.0;
    }

    private void randomizeParams() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        RM = random.nextDouble(Math.PI * 2);
        ayG = random.nextDouble(Math.PI * 2);
        SL = random.nextDouble(0.08, 0.25);
        Rt = random.nextDouble(0.06, 0.2);
        alh = random.nextDouble(1.8, 3.5);
        yY = random.nextDouble(0.75, 1.25);
        akx = random.nextDouble(-0.1, 0.2);
        adW = random.nextInt(3);
        Nk = random.nextDouble(-0.15, 0.1);
        aho = System.currentTimeMillis();
        updateMultipointParams(random);
    }

    private void updateMultipointParams(ThreadLocalRandom random) {
        vK = random.nextDouble(-0.35, 0.35);
        aiH = random.nextDouble(-0.12, 0.12);
        PK = random.nextDouble(-0.35, 0.35);
        KJ = System.currentTimeMillis() + random.nextLong(300L, 900L);
    }

    private void updateMicroMovements() {
        if (!microMovementsSetting.getValue()) return;
        long j = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (j - aho > random.nextLong(180L, 450L)) {
            Nk = Nk + random.nextDouble(-0.06, 0.06);
            Nk = MathHelper.clamp(Nk, -0.2, 0.15);
            aho = j;
        }
    }

    private void updateMultipoint() {
        if (!multipointSetting.getValue()) return;
        long j = System.currentTimeMillis();
        if (j >= KJ) {
            updateMultipointParams(ThreadLocalRandom.current());
        }

        if (multipointSetting.getValue()) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            double d1 = asf + (microMovementsSetting.getValue() ? random.nextDouble(-0.008, 0.008) : 0.0);
            all = all + (vK - all) * d1;
            ef = ef + (aiH - ef) * d1;
            aox = aox + (PK - aox) * d1;
        }
    }

    private double getAsf() {
        return ThreadLocalRandom.current().nextDouble(0.025, 0.065);
    }

    private void applyAimAssist() {
        long i = System.nanoTime();
        double d0 = aeD > 0L ? (i - aeD) / 1.66666667E7 : 1.0;
        d0 = MathHelper.clamp(d0, 0.05, 3.0);
        aeD = i;

        double[] targetAngles = calculateTargetAngles(FD);
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        boolean onlyX = onlyXSetting.getValue();

        double diffX = normalizeAngle(targetAngles[0] - currentYaw);
        double diffY = targetAngles[1] - currentPitch;

        double diffYLimited;
        if (onlyX) {
            double maxDiff = microMovementsSetting.getValue() ? 8.0 + ThreadLocalRandom.current().nextDouble(4.0) : 10.0;
            double absDiffY = Math.abs(diffY);
            if (absDiffY > maxDiff) {
                double step = microMovementsSetting.getValue() ? 0.06 + ThreadLocalRandom.current().nextDouble(0.06) : 0.08;
                diffYLimited = (diffY - Math.signum(diffY) * maxDiff) * step;
            } else {
                diffYLimited = 0.0;
            }
        } else {
            diffYLimited = diffY;
        }

        double diffMagnitude = onlyX ? Math.abs(diffX) : Math.sqrt(diffX * diffX + diffYLimited * diffYLimited);
        if (!(diffMagnitude < 0.05)) {
            long timeSinceTarget = System.currentTimeMillis() - Dp;
            boolean flag = microMovementsSetting.getValue();
            double d24 = MathHelper.clamp(timeSinceTarget / (flag ? 400.0 + ThreadLocalRandom.current().nextDouble(200.0) : 350.0), 0.0, 1.0);
            double d25 = getEasing(d24, flag);

            double baseSpeed = speedSetting.getValue().doubleValue();
            double speedMultiplier = baseSpeed * (flag ? yY : 1.0);
            if (flag && ThreadLocalRandom.current().nextDouble() < 0.015) {
                yY = ThreadLocalRandom.current().nextDouble(0.7, 1.3);
            }

            double distanceMultiplier;
            if (flag) {
                if (diffMagnitude < 3.0) distanceMultiplier = 0.35 + ThreadLocalRandom.current().nextDouble(0.25);
                else if (diffMagnitude < 10.0) distanceMultiplier = 0.7 + ThreadLocalRandom.current().nextDouble(0.3);
                else if (diffMagnitude < 25.0) distanceMultiplier = 0.85 + ThreadLocalRandom.current().nextDouble(0.3);
                else if (diffMagnitude < 50.0) distanceMultiplier = 1.15 + ThreadLocalRandom.current().nextDouble(0.35);
                else distanceMultiplier = 1.4 + ThreadLocalRandom.current().nextDouble(0.4);
            } else {
                if (diffMagnitude < 3.0) distanceMultiplier = 0.45;
                else if (diffMagnitude < 10.0) distanceMultiplier = 0.85;
                else if (diffMagnitude < 25.0) distanceMultiplier = 1.0;
                else if (diffMagnitude < 50.0) distanceMultiplier = 1.3;
                else distanceMultiplier = 1.6;
            }

            double fallMultiplier = 1.0;
            if (mc.player.fallDistance > 0.0F) {
                double fallFactor = flag ? 0.12 + ThreadLocalRandom.current().nextDouble(0.08) : 0.15;
                fallMultiplier = 1.0 + MathHelper.clamp(mc.player.fallDistance * fallFactor, 0.0, 0.7);
            }

            if (!mc.player.isOnGround() && mc.player.getVelocity().y > 0.1) {
                fallMultiplier *= flag ? 0.8 + ThreadLocalRandom.current().nextDouble(0.15) : 0.85;
            }

            double finalSpeed = speedMultiplier * d25 * distanceMultiplier * fallMultiplier * d0;

            double step = MathHelper.clamp(finalSpeed * 0.028, 0.005, 0.92);
            double moveX = diffX * step;
            double moveY = diffYLimited * step * (flag ? 0.85 + ThreadLocalRandom.current().nextDouble(0.3) : 1.0);

            if (flag && diffMagnitude < 2.5 && akx > 0.01) {
                double boost = akx * ThreadLocalRandom.current().nextDouble(0.4, 1.0);
                moveX *= 1.0 + boost;
                moveY *= 1.0 + boost * 0.7;
                akx = akx * (0.9 + ThreadLocalRandom.current().nextDouble(0.07));
            }

            if (flag) {
                double noise = 0.06 + ThreadLocalRandom.current().nextDouble(0.1);
                RM = RM + (SL + ThreadLocalRandom.current().nextDouble(-0.03, 0.03));
                ayG = ayG + (Rt + ThreadLocalRandom.current().nextDouble(-0.025, 0.025));
                moveX += Math.sin(RM) * noise * (0.5 + ThreadLocalRandom.current().nextDouble(0.8));
                moveY += Math.cos(ayG) * noise * 0.55 * (0.4 + ThreadLocalRandom.current().nextDouble(0.7));
            }

            double maxMoveX = flag ? 2.0 + ThreadLocalRandom.current().nextDouble(1.5) : 3.0;
            double maxMoveY = flag ? 1.5 + ThreadLocalRandom.current().nextDouble(1.0) : 2.5;

            double deltaX = moveX - aeX;
            double deltaY = moveY - nX;

            if (Math.abs(deltaX) > maxMoveX) {
                moveX = aeX + Math.signum(deltaX) * maxMoveX;
            }
            if (Math.abs(deltaY) > maxMoveY) {
                moveY = nX + Math.signum(deltaY) * maxMoveY;
            }

            aeX = moveX;
            nX = moveY;

            if (flag) {
                double smoothness = getSmoothness();
                moveX = applySmoothness(moveX, smoothness);
                moveY = applySmoothness(moveY, smoothness);
            }

            if (!(Math.abs(moveX) < 0.003) || !(Math.abs(moveY) < 0.003)) {
                float newYaw = currentYaw + (float) moveX;
                float newPitch = MathHelper.clamp(currentPitch + (float) moveY, -90.0F, 90.0F);

                if (!Float.isNaN(newYaw) && !Float.isNaN(newPitch) && !Float.isInfinite(newYaw) && !Float.isInfinite(newPitch)) {
                    mc.player.setYaw(newYaw);
                    mc.player.setPitch(newPitch);
                }
            }
        }
    }

    private double getSmoothness() {
        return 1.0;
    }

    private double applySmoothness(double value, double smoothness) {
        return value;
    }

    private double getAsfValue() {
        return ThreadLocalRandom.current().nextDouble(0.025, 0.065);
    }

    private double asf;

    {
        asf = getAsfValue();
    }

    private double getEasing(double t, boolean flag) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (flag) {
            double result;
            switch (adW) {
                case 0: {
                    double clamped = MathHelper.clamp(t, 0.0, 1.0);
                    result = 1.0 - Math.pow(1.0 - clamped, alh);
                    break;
                }
                case 1: {
                    double clamped = MathHelper.clamp(t, 0.0, 1.0);
                    if (clamped < 0.5) {
                        result = Math.pow(2.0 * clamped, alh) / 2.0;
                    } else {
                        result = 1.0 - Math.pow(2.0 * (1.0 - clamped), alh) / 2.0;
                    }
                    break;
                }
                default: {
                    double clamped = MathHelper.clamp(t, 0.0, 1.0);
                    result = clamped * clamped * (3.0 - 2.0 * clamped);
                    break;
                }
            }
            result *= (0.85 + random.nextDouble(0.3));
            return MathHelper.clamp(result, 0.05, 1.8);
        } else {
            double clamped = MathHelper.clamp(t, 0.0, 1.0);
            return 1.0 - Math.pow(1.0 - clamped, 2.5);
        }
    }

    private double[] calculateTargetAngles(Entity entity) {
        Vec3d playerPos = mc.player.getEyePos();
        boolean flag = microMovementsSetting.getValue();
        double heightValue = aimHeightSetting.getValue().doubleValue();
        double adjustedHeight = flag ? heightValue + Nk : heightValue;

        double offsetX = 0.0;
        double offsetY = 0.0;
        double offsetZ = 0.0;

        if (multipointSetting.getValue()) {
            double halfWidth = entity.getWidth() * 0.5;
            offsetX = all * halfWidth;
            offsetZ = aox * halfWidth;
            offsetY = ef;
        }

        double finalHeight = MathHelper.clamp(adjustedHeight + offsetY, 0.05, 0.95);
        Vec3d targetPos = entity.getPos().add(offsetX, entity.getHeight() * finalHeight, offsetZ);

        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double yaw;
        if (horizontalDist < 0.001) {
            yaw = mc.player.getYaw();
        } else {
            yaw = Math.toDegrees(Math.atan2(-dx, dz));
        }

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double pitch;
        if (distance < 0.001) {
            pitch = mc.player.getPitch();
        } else {
            pitch = -Math.toDegrees(Math.asin(MathHelper.clamp(dy / distance, -1.0, 1.0)));
        }

        return new double[]{yaw, pitch};
    }

    private double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    

    private Entity findBestTarget() {
        if (mc.player == null || mc.world == null) return null;

        double maxDistance = distanceSetting.getValue().doubleValue();
        double halfFov = fovSetting.getValue().doubleValue() / 2.0;
        boolean prioritizeHealth = "Health".equals(prioritySetting.getValue());

        Entity bestEntity = null;
        double bestValue = Double.MAX_VALUE;

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity livingEntity)) continue;
            if (!livingEntity.isAlive()) continue;
            if (livingEntity.getHealth() <= 0.0F) continue;

            if (onlyPlayersSetting.getValue() && !(entity instanceof PlayerEntity)) continue;
            if (entity instanceof PlayerEntity player && isFriend(player)) continue;
            if (!hitInvisibleSetting.getValue() && livingEntity.isInvisible()) continue;

            if (onlyArmoredSetting.getValue()) {
                if (!isArmored(livingEntity)) continue;
            } else if (ignoreNakedSetting.getValue() && !isArmored(livingEntity)) {
                continue;
            }

            double distance = mc.player.distanceTo(entity);
            if (distance > maxDistance || distance < 0.5) continue;

            if (wallCheckSetting.getValue()) {
                Vec3d targetPos = entity.getPos().add(0.0, entity.getHeight() * aimHeightSetting.getValue(), 0.0);
                if (!canSee(playerPos, targetPos)) continue;
            }

            double angle = getAngleToEntity(playerPos, lookVec, entity);
            if (angle > halfFov) continue;

            double value;
            if (prioritizeHealth) {
                value = livingEntity.getHealth();
            } else {
                value = distance;
            }

            if (value < bestValue) {
                bestValue = value;
                bestEntity = entity;
            }
        }

        return bestEntity;
    }

    private double getAngleToEntity(Vec3d playerPos, Vec3d lookVec, Entity entity) {
        Vec3d targetPos = entity.getPos().add(0.0, entity.getHeight() * aimHeightSetting.getValue(), 0.0);
        Vec3d direction = targetPos.subtract(playerPos);
        double length = direction.length();
        if (length < 0.001) return 0.0;

        direction = direction.multiply(1.0 / length);
        double dot = lookVec.x * direction.x + lookVec.y * direction.y + lookVec.z * direction.z;
        dot = MathHelper.clamp(dot, -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
    }

    private boolean canSee(Vec3d from, Vec3d to) {
        return PlayerUtil.canSee(to);
    }

    private boolean isArmored(LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.FEET).isEmpty() &&
                entity.getEquippedStack(EquipmentSlot.LEGS).isEmpty() &&
                entity.getEquippedStack(EquipmentSlot.CHEST).isEmpty() &&
                entity.getEquippedStack(EquipmentSlot.HEAD).isEmpty();
    }

    private boolean isFriend(PlayerEntity player) {
        return false;
    }

    private boolean hasWeapon() {
        if (mc.player == null) return false;
        Item item = mc.player.getMainHandStack().getItem();
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
    }
}
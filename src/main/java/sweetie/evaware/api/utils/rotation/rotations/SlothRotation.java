package sweetie.evaware.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;
import sweetie.evaware.client.features.modules.combat.AuraModule;

import java.util.Random;

/**
 * Sloth rotation — плавные мультипоинты по body + джиттер + плавный флик + humanize.
 * Полная портация из CloudClient.
 */
public class SlothRotation extends RotationMode implements QuickImports {

    private final Random slothRandom = new Random();
    
    // Целевая точка на теле (мультипоинты по body)
    private double slothTargetH  = 0.5;
    private double slothTargetOX = 0.0;
    private double slothTargetOZ = 0.0;
    // Интерполированная точка
    private double slothCurrentH  = 0.5;
    private double slothCurrentOX = 0.0;
    private double slothCurrentOZ = 0.0;
    // Таймер смены мультипоинта
    private long slothLastPointSwitch = 0;
    private long slothPointInterval = 100;
    private LivingEntity slothLastTarget = null;
    // Плавный джиттер
    private float slothJitterYaw         = 0f;
    private float slothJitterPitch       = 0f;
    private float slothJitterTargetYaw   = 0f;
    private float slothJitterTargetPitch = 0f;
    private long slothLastJitterSwitch = 0;
    // Флик после удара — плавно нарастает и затухает
    private float slothFlickYaw        = 0f;
    private float slothFlickPitch      = 0f;
    private float slothFlickTargetYaw  = 0f;
    private float slothFlickTargetPitch= 0f;
    // Авто-флики за хитбокс — небольшие, часто
    private long slothLastAutoFlick = 0;
    private long slothAutoFlickInterval = 300;
    // Humanize — случайный множитель скорости ротации
    private float slothHumanSpeed      = 1.0f;
    private float slothHumanSpeedTarget= 1.0f;
    // Счётчик ударов для флика
    private int slothAttackCount = 0;
    private long slothLastAttackTime = 0;

    public SlothRotation() {
        super("Sloth");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AuraModule aura = AuraModule.getInstance();

        if (entity == null || !(entity instanceof LivingEntity target)) {
            // Плавное возвращение камеры когда entity == null (таргет потерян / модуль отключён)
            Rotation cameraRotation = RotationUtil.fromVec2f(mc.player.getRotationClient());
            Rotation delta = RotationUtil.calculateDelta(currentRotation, cameraRotation);
            float yawDelta = delta.getYaw();
            float pitchDelta = delta.getPitch();
            float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

            float speedFactor = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.05f, 0.4f);
            float speed = 0.35F * speedFactor;

            float lineYaw = rotationDifference > 0 ? (Math.abs(yawDelta / rotationDifference) * 360) : 360;
            float linePitch = rotationDifference > 0 ? (Math.abs(pitchDelta / rotationDifference) * 180) : 180;

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            return new Rotation(
                    MathHelper.lerp(speed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw),
                    MathHelper.lerp(speed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch)
            );
        }

        // Сброс при смене цели
        if (target != slothLastTarget) {
            slothLastTarget = target;
            slothPickNewPoint(true);
            slothJitterYaw = 0f;
            slothJitterPitch = 0f;
            slothJitterTargetYaw = 0f;
            slothJitterTargetPitch = 0f;
            slothFlickYaw = 0f;
            slothFlickPitch = 0f;
            slothFlickTargetYaw = 0f;
            slothFlickTargetPitch = 0f;
            slothHumanSpeed = 1.0f;
            slothHumanSpeedTarget = 1.0f;
            slothAutoFlickInterval = 300;
            slothAttackCount = 0;
            slothLastAttackTime = System.currentTimeMillis();
        }

        long currentTime = System.currentTimeMillis();

        // Смена мультипоинта каждые 50–120 мс
        if (currentTime - slothLastPointSwitch > slothPointInterval) {
            slothPickNewPoint(false);
        }

        // Humanize — случайно меняем скорость ротации (0.55–1.0), плавно интерполируем
        if (slothRandom.nextFloat() < 0.04f) {
            slothHumanSpeedTarget = 0.55f + slothRandom.nextFloat() * 0.45f;
        }
        slothHumanSpeed = MathHelper.lerp(0.07f, slothHumanSpeed, slothHumanSpeedTarget);

        // Плавная интерполяция к мультипоинту с humanize-скоростью
        float lerpSpeed = 0.18f * slothHumanSpeed;
        slothCurrentH  = MathHelper.lerp(lerpSpeed, (float) slothCurrentH,  (float) slothTargetH);
        slothCurrentOX = MathHelper.lerp(lerpSpeed, (float) slothCurrentOX, (float) slothTargetOX);
        slothCurrentOZ = MathHelper.lerp(lerpSpeed, (float) slothCurrentOZ, (float) slothTargetOZ);

        // Точка прицела на теле
        Vec3d aimPoint = target.getPos().add(
                slothCurrentOX,
                target.getHeight() * slothCurrentH,
                slothCurrentOZ
        );

        Rotation targetAngle = RotationUtil.fromVec3d(aimPoint.subtract(mc.player.getEyePos()));

        // Плавный джиттер — меняется каждые 80–160 мс, амплитуда ±4° yaw / ±2.5° pitch
        if (currentTime - slothLastJitterSwitch > 80 + (long)(slothRandom.nextFloat() * 80)) {
            slothJitterTargetYaw   = (slothRandom.nextFloat() - 0.5f) * 8f;
            slothJitterTargetPitch = (slothRandom.nextFloat() - 0.5f) * 5f;
            slothLastJitterSwitch = currentTime;
        }
        slothJitterYaw   = MathHelper.lerp(0.12f, slothJitterYaw,   slothJitterTargetYaw);
        slothJitterPitch = MathHelper.lerp(0.12f, slothJitterPitch, slothJitterTargetPitch);

        // Авто-флики за хитбокс — небольшие, каждые 200–500 мс
        if (currentTime - slothLastAutoFlick > slothAutoFlickInterval) {
            // Накапливаем поверх текущего target (суммируем, не перезаписываем)
            slothFlickTargetYaw   += (slothRandom.nextFloat() - 0.5f) * 22f; // ±11°
            slothFlickTargetPitch += (slothRandom.nextFloat() - 0.5f) * 14f; // ±7°
            slothAutoFlickInterval = 200 + (long)(slothRandom.nextFloat() * 300);
            slothLastAutoFlick = currentTime;
        }

        // Флик — плавно нарастает к target (0.25 за тик), затем медленно затухает (0.88)
        slothFlickYaw   = MathHelper.lerp(0.25f, slothFlickYaw,   slothFlickTargetYaw);
        slothFlickPitch = MathHelper.lerp(0.25f, slothFlickPitch, slothFlickTargetPitch);
        // Затухание target к нулю — флик "уходит" обратно
        slothFlickTargetYaw   *= 0.88f;
        slothFlickTargetPitch *= 0.88f;

        // Получаем дельту между текущей ротацией и целевой
        Rotation delta = currentRotation.rotationDeltaTo(targetAngle);
        float yawDelta   = delta.getYaw();
        float pitchDelta = delta.getPitch();

        // Добавляем джиттер и флик
        float finalYawDelta   = yawDelta + slothJitterYaw + slothFlickYaw;
        float finalPitchDelta = pitchDelta + slothJitterPitch + slothFlickPitch;

        // Humanize — ускоряем при большом угле (как человек, который "догоняет" цель)
        float dist = (float) Math.hypot(finalYawDelta, finalPitchDelta);
        float accel = MathHelper.clamp(dist / 30f, 0f, 1f); // 0 при малом угле, 1 при >30°
        float finalSpeed = MathHelper.lerp(accel, slothHumanSpeed, 1.0f);

        float newYaw   = currentRotation.getYaw() + finalYawDelta * finalSpeed;
        float newPitch = MathHelper.clamp(currentRotation.getPitch() + finalPitchDelta * finalSpeed, -90f, 90f);

        return new Rotation(newYaw, newPitch);
    }

    /** Обрабатывает удар для флик-эффекта */
    public void onAttack() {
        slothFlickTargetYaw   = (slothRandom.nextFloat() - 0.5f) * 36f; // ±18°
        slothFlickTargetPitch = (slothRandom.nextFloat() - 0.5f) * 20f; // ±10°
        slothAttackCount++;
        slothLastAttackTime = System.currentTimeMillis();
    }

    /** Выбирает новый мультипоинт строго по body (0.30–0.70 высоты). */
    private void slothPickNewPoint(boolean instant) {
        if (slothLastTarget == null) return;

        slothTargetH  = 0.30 + slothRandom.nextDouble() * 0.40;
        slothTargetOX = (slothRandom.nextDouble() - 0.5) * 0.25;
        slothTargetOZ = (slothRandom.nextDouble() - 0.5) * 0.25;

        if (instant) {
            slothCurrentH  = slothTargetH;
            slothCurrentOX = slothTargetOX;
            slothCurrentOZ = slothTargetOZ;
        }

        // Следующий мультипоинт через 50–120 мс
        slothPointInterval = 50 + (long)(slothRandom.nextFloat() * 70);
        slothLastPointSwitch = System.currentTimeMillis();
    }

    /** Сбрасывает состояние при отключении */
    public void reset() {
        slothFlickYaw = 0f;
        slothFlickPitch = 0f;
        slothFlickTargetYaw = 0f;
        slothFlickTargetPitch = 0f;
        slothHumanSpeed = 1.0f;
        slothHumanSpeedTarget = 1.0f;
        slothJitterYaw = 0f;
        slothJitterPitch = 0f;
        slothJitterTargetYaw = 0f;
        slothJitterTargetPitch = 0f;
        slothLastTarget = null;
        slothAttackCount = 0;
        slothLastAttackTime = 0;
        slothAutoFlickInterval = 300;
    }
}

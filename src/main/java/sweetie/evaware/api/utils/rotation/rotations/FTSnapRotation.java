package sweetie.evaware.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.player.MoveUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;
import sweetie.evaware.client.features.modules.combat.AuraModule;

import java.security.SecureRandom;

/**
 * FunTime Snap — полный перенос логики FTAngle из Rich-Modern.
 * Включает: джиттер-систему, обработку промахов, swing-анимацию,
 * адаптивную скорость и случайное сглаживание.
 */
public class FTSnapRotation extends RotationMode {

    private static final SecureRandom random = new SecureRandom();

    // Статические поля для отслеживания состояния атак (как в Rich-Modern)
    private static int lastCount = -1;
    private static int hitsAfterMiss = 0;
    private static long missEndTime = 0;
    private static int swingsDone = 0;
    private static int attackCount = 0;
    private static long lastAttackTime = 0;

    // Внутренние поля джиттера
    private float currentJitterYaw = 0;
    private float currentJitterPitch = 0;
    private float targetJitterYaw = 0;
    private float targetJitterPitch = 0;

    public FTSnapRotation() {
        super("Ft snap");
    }

    public static void updateAttackState(boolean attacking) {
        if (attacking) {
            attackCount++;
            lastAttackTime = System.currentTimeMillis();
        }
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AuraModule aura = AuraModule.getInstance();
        long now = System.currentTimeMillis();

        // Отслеживание количества атак (аналог count из StrikeManager)
        int count = attackCount;

        // --- Логика восстановления после промаха (miss recovery) ---
        if (count != lastCount) {
            hitsAfterMiss++;
            lastCount = count;
        }

        if (hitsAfterMiss >= 40 && missEndTime == 0) {
            missEndTime = now + 350;
            hitsAfterMiss = 0;
            swingsDone = 0;
        }

        if (missEndTime != 0) {
            if (now < missEndTime) {
                long elapsed = now - (missEndTime - 350);
                if (swingsDone == 0 && elapsed >= 50) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    swingsDone = 1;
                } else if (swingsDone == 1 && elapsed >= 180) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    swingsDone = 2;
                }
                // Во время miss recovery — возвращаем случайный угол с питчем -80
                return new Rotation(currentRotation.getYaw() + random.nextFloat() * 6 - 3, -80);
            } else {
                missEndTime = 0;
            }
        }

        // --- Расчёт дельты углов ---
        Rotation angleDelta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.01f) rotationDifference = 1;

        // --- Случайный паттерн джиттера (switch suck) ---
        long attackTimerElapsed = now - lastAttackTime;
        float timeRandom = attackTimerElapsed / 80F + (count % 6);
        int suck = count % 3;

        Rotation randomAngle = switch (suck) {
            case 0 -> new Rotation((float) Math.cos(timeRandom), (float) Math.sin(timeRandom));
            case 1 -> new Rotation((float) Math.sin(timeRandom), (float) Math.cos(timeRandom));
            case 2 -> new Rotation((float) Math.sin(timeRandom), (float) -Math.cos(timeRandom));
            default -> new Rotation((float) -Math.cos(timeRandom), (float) Math.sin(timeRandom));
        };

        // --- Целевые значения джиттера ---
        targetJitterYaw = randomLerp(11, 20) * randomAngle.getYaw();
        // Pitch джиттер — всегда отрицательный или близкий к нулю (камера вниз)
        targetJitterPitch = -(randomLerp(1, 6) * Math.abs(randomAngle.getPitch())
                + randomLerp(2, 1) * (float) Math.cos(now / 8000.0));

        // --- Сглаживание джиттера ---
        float jitterSmoothSpeed = 1f;
        currentJitterYaw += (targetJitterYaw - currentJitterYaw) * jitterSmoothSpeed;
        currentJitterPitch += (targetJitterPitch - currentJitterPitch) * jitterSmoothSpeed;
        // Гарантируем что jitterPitch никогда не станет положительным (> -1)
        currentJitterPitch = Math.min(currentJitterPitch, -1f);

        // --- Финальный расчёт вращения ---
        if (entity != null && aura.target != null) {
            // Цель валидна — атакуем
            boolean canAttack = aura.combatExecutor.combatManager().canAttack();
            float speed = canAttack ? 0.9f : (random.nextBoolean() ? 0.1F : 0.2F);

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float lerpSpeed = randomLerp(speed, speed + 0.6F);

            // При атаке НЕ добавляем джиттер — только чистое наведение на цель
            float newYaw = MathHelper.lerp(lerpSpeed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw);
            float newPitch = MathHelper.lerp(lerpSpeed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch);
            
            // Джиттер только когда НЕ атакуем
            if (!canAttack) {
                newYaw += currentJitterYaw;
                newPitch += currentJitterPitch;
            }

            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90, 90));

        } else {
            // Нет цели — используем таймер атаки для определения скорости
            float speed = attackTimerElapsed > 650 ? (random.nextBoolean() ? 0.85F : 0.2F) : -0.2F;

            float yawJitter = attackTimerElapsed < 2000 ? currentJitterYaw : 0;
            float pitchJitter = attackTimerElapsed < 2000 ? currentJitterPitch : 0;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float lerpSpeed = MathHelper.clamp(randomLerp(speed, speed + 0.2F), 0, 1);

            float newYaw = MathHelper.lerp(lerpSpeed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw) + yawJitter;
            float newPitch = MathHelper.lerp(lerpSpeed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch) + pitchJitter;

            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90, 90));
        }
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }
}

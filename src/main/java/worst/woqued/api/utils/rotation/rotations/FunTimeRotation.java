package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AuraModule;

import java.util.Random;

public class FunTimeRotation extends RotationMode {
    public static boolean attack;
    public static int attackCount = 0;

    private static final float[] SWING_THRESHOLDS = new float[]{50f, 230f};

    private static int lastCount = -1;
    private static int hitsAfterMiss = 0;
    private static long missEndTime = 0;

    private float currentJitterYaw = 0;
    private float currentJitterPitch = 0;
    private float targetJitterYaw = 0;
    private float targetJitterPitch = 0;

    private final Random random = new Random();

    private int swingStage = 0;
    private long swingTimer = 0;

    public FunTimeRotation() {
        super("FunTime");
    }

    public static void updateAttackState(boolean attack) {
        FunTimeRotation.attack = attack;
    }

    public void startRelease() {
        if (swingStage == 0) {
            swingStage = 1;
            swingTimer = System.currentTimeMillis();
        }
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        long now = System.currentTimeMillis();
        var combatManager = AuraModule.getInstance().combatExecutor.combatManager();

        if (combatManager.canAttack()) {
            if (lastCount != -1 && lastCount != attackCount) {
                hitsAfterMiss++;
            }
            lastCount = attackCount;
        } else {
            lastCount = -1;
        }

        if (hitsAfterMiss >= 40 && missEndTime == 0) {
            missEndTime = now + 350;
            hitsAfterMiss = 0;
            swingStage = 0;
        }

        if (missEndTime != 0) {
            if (now < missEndTime) {
                long missedElapsed = now - (missEndTime - 350);
                if (swingStage == 0 && missedElapsed >= SWING_THRESHOLDS[0]) {
                    mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    swingStage = 1;
                } else if (swingStage == 1 && missedElapsed >= SWING_THRESHOLDS[1]) {
                    mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    swingStage = 2;
                }
                return new Rotation(currentRotation.getYaw() + random.nextFloat() * 6 - 3, -80);
            } else {
                missEndTime = 0;
                swingStage = 0;
            }
        }

        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.01f) rotationDifference = 1;

        int suck = attackCount % 3;
        long elapsed = combatManager.clickScheduler.lastClickPassed();
        float timeRandom = elapsed / 80f + (attackCount % 6);

        float randomYaw = switch (suck) {
            case 0 -> (float) Math.cos(timeRandom);
            case 1 -> (float) Math.sin(timeRandom);
            case 2 -> (float) Math.sin(timeRandom);
            default -> (float) -Math.cos(timeRandom);
        };

        float randomPitch = switch (suck) {
            case 0 -> (float) Math.sin(timeRandom);
            case 1 -> (float) Math.cos(timeRandom);
            case 2 -> (float) -Math.cos(timeRandom);
            default -> (float) Math.sin(timeRandom);
        };

        targetJitterYaw = randomLerp(11, 20) * randomYaw;
        targetJitterPitch = randomLerp(1, 6) * randomPitch + randomLerp(2, 1) * (float) Math.cos(System.currentTimeMillis() / 8000.0);

        float jitterSmoothSpeed = 1f;
        currentJitterYaw += (targetJitterYaw - currentJitterYaw) * jitterSmoothSpeed;
        currentJitterPitch += (targetJitterPitch - currentJitterPitch) * jitterSmoothSpeed;

        if (entity != null) {
            float speed = combatManager.canAttack() ? 0.9f : (random.nextBoolean() ? 0.1f : 0.2f);

            float lineYaw = Math.abs(yawDelta / rotationDifference) * 180;
            float linePitch = Math.abs(pitchDelta / rotationDifference) * 180;

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float lerpSpeed = randomLerp(speed, speed + 0.6f);

            float newYaw = MathHelper.lerp(lerpSpeed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw) + currentJitterYaw;
            float newPitch = MathHelper.lerp(lerpSpeed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch) + currentJitterPitch;

            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90, 90));
        } else {
            float speed = elapsed > 650 ? (random.nextBoolean() ? 0.85f : 0.2f) : -0.2f;

            float yawJitter = elapsed < 2000 ? currentJitterYaw : 0;
            float pitchJitter = elapsed < 2000 ? currentJitterPitch : 0;

            float lineYaw = Math.abs(yawDelta / rotationDifference) * 180;
            float linePitch = Math.abs(pitchDelta / rotationDifference) * 180;

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float lerpSpeed = (float) Math.clamp(randomLerp(speed, speed + 0.2f), 0, 1);

            float newYaw = MathHelper.lerp(lerpSpeed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw) + yawJitter;
            float newPitch = MathHelper.lerp(lerpSpeed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch) + pitchJitter;

            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90, 90));
        }
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }
}
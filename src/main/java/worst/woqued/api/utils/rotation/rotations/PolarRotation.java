package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AuraModule;

public class PolarRotation extends RotationMode {

    private static final int HISTORY_SIZE = 8;
    private final double[] pitchHistory = new double[HISTORY_SIZE];
    private int historyIndex = 0;

    private static final double MU1 = 2.384;
    private static final double MU2 = 3.384;
    private static final double SIGMA1 = 12.541;
    private static final double SIGMA2 = 14.210;
    private static final double ALPHA = 0.6;

    private static final double AUTOREGRESSIVE_BETA = 0.15;
    private static final double NOISE_GAMMA = 0.8;
    private static final double COMPENSATION_FACTOR = 0.1;

    public PolarRotation() {
        super("Polar");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AuraModule aura = AuraModule.getInstance();
        boolean canAttack = entity != null && aura.combatExecutor.combatManager().canAttack();
        boolean hasTarget = entity != null && aura.target != null;

        if (canAttack && entity != null) {
            float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
            float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

            float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

            if (rotationDifference > 180f) {
                return targetRotation;
            }

            float speed = Math.min(1.0f, rotationDifference / 20f);
            float lerpFactor = 0.5f + speed * 0.5f;

            float baseYaw = MathHelper.lerp(lerpFactor, currentRotation.getYaw(), targetRotation.getYaw());
            float basePitch = MathHelper.lerp(lerpFactor, currentRotation.getPitch(), targetRotation.getPitch());

            double[] deltaP = computeDeltaP(currentRotation.getPitch(), targetRotation.getPitch(), basePitch);

            double stochastics = deltaP[0];
            double compensation = deltaP[1];

            float newYaw = baseYaw + (float) ((Math.random() - 0.5) * 3.0 * (1 + Math.random() * 0.5));
            float newPitch = (float) (basePitch + stochastics + compensation);

            return new Rotation(newYaw, newPitch);
        }

        if (hasTarget) {
            float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
            float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

            float speed = 0.3f;

            float yawJitter = (float) ((Math.random() - 0.5) * 3.0);
            float pitchJitter = (float) ((Math.random() - 0.5) * 2.5);

            return new Rotation(
                    currentRotation.getYaw() + yawDelta * speed + yawJitter,
                    currentRotation.getPitch() + pitchDelta * speed + pitchJitter
            );
        }

        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        float speedFactor = MathHelper.clamp(1f - (rotationDifference / 180f), 0.05f, 0.4f);
        float speed = 0.35f * speedFactor;

        float lineYaw = rotationDifference > 0 ? (Math.abs(yawDelta / rotationDifference) * 360) : 360;
        float linePitch = rotationDifference > 0 ? (Math.abs(pitchDelta / rotationDifference) * 180) : 180;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        return new Rotation(
                MathHelper.lerp(speed, currentRotation.getYaw(), currentRotation.getYaw() + moveYaw),
                MathHelper.lerp(speed, currentRotation.getPitch(), currentRotation.getPitch() + movePitch)
        );
    }

    private double[] computeDeltaP(float currentPitch, float targetPitch, float basePitch) {
        double deltaBase = targetPitch - currentPitch;

        double rand = Math.random();
        double stochastics;
        if (rand < ALPHA) {
            stochastics = gaussianSample(MU1, SIGMA1);
        } else {
            stochastics = gaussianSample(MU2, SIGMA2);
        }

        double autoregressive = 0;
        for (int i = 0; i < HISTORY_SIZE; i++) {
            int idx = (historyIndex - 1 - i + HISTORY_SIZE) % HISTORY_SIZE;
            autoregressive += pitchHistory[idx] * AUTOREGRESSIVE_BETA * Math.pow(0.7, i);
        }

        double noise = gaussianSample(0, 1) * NOISE_GAMMA;
        double compensation = -deltaBase * COMPENSATION_FACTOR;

        double totalDelta = deltaBase + stochastics + autoregressive + noise + compensation;

        pitchHistory[historyIndex] = totalDelta;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;

        return new double[]{stochastics + noise, compensation};
    }

    private double gaussianSample(double mean, double stdDev) {
        double u1 = Math.random();
        double u2 = Math.random();
        double z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        return z0 * stdDev + mean;
    }
}
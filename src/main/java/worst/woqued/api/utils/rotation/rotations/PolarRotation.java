package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AuraModule;

import java.util.Random;

/**
 * PolarRotation implements a mathematically optimized rotation strategy
 * based on deltaPitch optimization for bypassing neural anti-cheats.
 * 
 * Core Equation: ΔP(t) = ΔP_base(t) + ε(t) + C(t)
 * where:
 * - ΔP_base(t) is the deterministic base rotation towards target
 * - ε(t) ~ N(0, σ²(t)) is stochastic noise (Gaussian mixture)
 * - C(t) is a compensation term for prediction/tracking errors
 * 
 * The strategy uses an autoregressive model to simulate human-like
 * temporal dependencies and a mixture of Gaussians for noise distribution.
 */
public class PolarRotation extends RotationMode {

    private static final int HISTORY_SIZE = 10;
    private final double[] pitchHistory = new double[HISTORY_SIZE];
    private int historyIndex = 0;

    // Gaussian Mixture Parameters for ε(t)
    private static final double MU1 = 0.0;
    private static final double MU2 = 0.0;
    private static final double SIGMA1 = 1.2;
    private static final double SIGMA2 = 3.5;
    private static final double ALPHA = 0.75;

    // Autoregressive & Noise Parameters
    private static final double AUTOREGRESSIVE_BETA = 0.12;
    private static final double DECAY_FACTOR = 0.65;
    private static final double NOISE_GAMMA = 0.9;
    
    // Compensation & Smoothing
    private static final double COMPENSATION_FACTOR = 0.08;
    private static final double SMOOTHING_LERP = 0.82;

    private final Random random = new Random();
    private double lastDeltaPitch = 0.0;

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

            // ΔP_base(t): Deterministic base rotation
            float speed = Math.min(1.0f, rotationDifference / 25f);
            float lerpFactor = 0.6f + speed * 0.4f;
            float baseYaw = MathHelper.lerp(lerpFactor, currentRotation.getYaw(), targetRotation.getYaw());
            float basePitch = MathHelper.lerp(lerpFactor, currentRotation.getPitch(), targetRotation.getPitch());

            // Compute stochastic and compensation components
            double[] components = computeDeltaP(currentRotation.getPitch(), targetRotation.getPitch(), basePitch);
            double stochasticNoise = components[0];
            double compensation = components[1];

            // Apply noise to yaw as well for natural movement
            float yawNoise = (float) ((random.nextGaussian()) * 1.5);
            
            // Final rotation: ΔP(t) = ΔP_base(t) + ε(t) + C(t)
            float newYaw = baseYaw + yawNoise;
            float newPitch = (float) MathHelper.clamp(basePitch + stochasticNoise + compensation, -90f, 90f);

            return new Rotation(newYaw, newPitch);
        }

        if (hasTarget) {
            // Smooth tracking when target exists but can't attack
            float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
            float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();
            float speed = 0.35f;

            float yawJitter = (float) (random.nextGaussian() * 2.0);
            float pitchJitter = (float) (random.nextGaussian() * 1.5);

            return new Rotation(
                    currentRotation.getYaw() + yawDelta * speed + yawJitter,
                    currentRotation.getPitch() + pitchDelta * speed + pitchJitter
            );
        }

        // Default smooth return when no target
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

    /**
     * Computes the delta pitch components based on the mathematical model:
     * ΔP(t) = ΔP_base(t) + ε(t) + C(t)
     */
    private double[] computeDeltaP(float currentPitch, float targetPitch, float basePitch) {
        double deltaBase = targetPitch - currentPitch;

        // ε(t): Stochastic noise from Gaussian Mixture
        double stochasticNoise;
        if (random.nextDouble() < ALPHA) {
            stochasticNoise = gaussianSample(MU1, SIGMA1);
        } else {
            stochasticNoise = gaussianSample(MU2, SIGMA2);
        }

        // Autoregressive component modeling temporal dependency
        double autoregressive = 0.0;
        for (int i = 0; i < HISTORY_SIZE; i++) {
            int idx = (historyIndex - 1 - i + HISTORY_SIZE) % HISTORY_SIZE;
            autoregressive += pitchHistory[idx] * AUTOREGRESSIVE_BETA * Math.pow(DECAY_FACTOR, i);
        }

        // Additional white noise
        double whiteNoise = random.nextGaussian() * NOISE_GAMMA;

        // C(t): Compensation term for tracking error
        double compensation = -deltaBase * COMPENSATION_FACTOR;

        // Total delta pitch
        double totalDelta = deltaBase + stochasticNoise + autoregressive + whiteNoise + compensation;

        // Update history
        pitchHistory[historyIndex] = totalDelta;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        lastDeltaPitch = totalDelta;

        return new double[]{stochasticNoise + whiteNoise, compensation};
    }

    private double gaussianSample(double mean, double stdDev) {
        return random.nextGaussian() * stdDev + mean;
    }
}

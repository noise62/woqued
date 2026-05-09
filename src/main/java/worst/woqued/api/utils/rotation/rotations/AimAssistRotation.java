package worst.woqued.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.client.features.modules.combat.AimAssistModule;

public class AimAssistRotation extends RotationMode {

    private float lastPlayerYaw;
    private float lastPlayerPitch;
    private boolean initialized = false;
    private boolean returningToTarget = false;
    private long returnStartTime = 0;

    private static final float RETURN_SPEED = 0.15f;

    public AimAssistRotation() {
        super("AimAssist");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        AimAssistModule aimAssist = AimAssistModule.getInstance();
        
        if (!aimAssist.isEnabled()) {
            return targetRotation;
        }

        float playerYaw = mc.player.getYaw();
        float playerPitch = mc.player.getPitch();

        if (!initialized) {
            lastPlayerYaw = playerYaw;
            lastPlayerPitch = playerPitch;
            initialized = true;
        }

        float playerYawDelta = MathHelper.wrapDegrees(playerYaw - lastPlayerYaw);
        float playerPitchDelta = playerPitch - lastPlayerPitch;

        boolean playerMovingCamera = Math.abs(playerYawDelta) > 0.5f || Math.abs(playerPitchDelta) > 0.3f;

        lastPlayerYaw = playerYaw;
        lastPlayerPitch = playerPitch;

        if (playerMovingCamera) {
            if (!returningToTarget) {
                returningToTarget = true;
                returnStartTime = System.currentTimeMillis();
            }
        } else if (returningToTarget) {
            boolean stillAimingAway = Math.abs(playerYawDelta) > 0.05f || Math.abs(playerPitchDelta) > 0.05f;
            if (!stillAimingAway) {
                returningToTarget = false;
            }
        }

        if (aimAssist.isEnabled() && entity != null) {
            float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
            float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

            float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

            if (returningToTarget) {
                float timeSinceReturn = (System.currentTimeMillis() - returnStartTime) / 1000f;
                float smoothProgress = Math.min(1.0f, timeSinceReturn * 1.5f);

                float progressSpeed = RETURN_SPEED * smoothProgress;

                float progressYaw = currentRotation.getYaw() + yawDelta * progressSpeed;
                float progressPitch = currentRotation.getPitch() + pitchDelta * progressSpeed;

                return new Rotation(progressYaw, progressPitch);
            }

            if (rotationDifference > 180f) {
                return targetRotation;
            }

            float aimSpeed = aimAssist.getAimSpeed().getValue();
            boolean autoPitch = aimAssist.getAutoPitch().getValue();

            float speedFactor = Math.min(1.0f, rotationDifference / 30.0f);
            float lerpFactor = (aimSpeed / 60.0f) * (0.3f + speedFactor * 0.7f);

            if (!autoPitch) {
                float clampedPitch = MathHelper.clamp(targetRotation.getPitch(), -15.0f, 15.0f);
                pitchDelta = clampedPitch - currentRotation.getPitch();
            }

            float maxYawPerTick = aimSpeed * 1.5f;
            float maxPitchPerTick = aimSpeed * 1.2f;

            float moveYaw = MathHelper.clamp(yawDelta, -maxYawPerTick, maxYawPerTick);
            float movePitch = MathHelper.clamp(pitchDelta, -maxPitchPerTick, maxPitchPerTick);

            return new Rotation(
                    currentRotation.getYaw() + moveYaw * lerpFactor,
                    currentRotation.getPitch() + movePitch * lerpFactor
            );
        }

        return new Rotation(playerYaw, playerPitch);
    }
}
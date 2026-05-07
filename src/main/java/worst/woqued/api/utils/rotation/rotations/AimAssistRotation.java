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
            return new Rotation(playerYaw, playerPitch);
        }

        float aimSpeed = aimAssist.getAimSpeed().getValue();
        boolean autoPitch = aimAssist.getAutoPitch().getValue();

        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference > 180f) {
            return targetRotation;
        }

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
}
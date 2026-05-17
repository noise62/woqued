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
    public static final FunTimeRotation INSTANCE = new FunTimeRotation();
    private long smoothbackShakeStartMs = -1L;
    private final Random random = new Random();

    public FunTimeRotation() {
        super("FunTimeSnap");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d targetVec, Entity targetEntity) {
        AuraModule aura = AuraModule.getInstance();

        if (aura != null && mc.player != null) {
            if (aura.isEnabled() && aura.target != null && targetEntity != null) {
                this.smoothbackShakeStartMs = -1L;

                Rotation diff = RotationUtil.calculateDelta(currentRotation, targetRotation);
                float yawDiff = diff.getYaw();
                float pitchDiff = diff.getPitch();

                float rotationDistance = calculateRotationDistance(yawDiff, pitchDiff);
                float maxStepYaw = Math.abs(yawDiff / rotationDistance) * 30.0F;
                float maxStepPitch = Math.abs(pitchDiff / rotationDistance) * 30.0F;

                float shakeYaw = randomInt(8, 11) * (float) Math.sin(System.currentTimeMillis() / 55.0);
                float shakePitch = randomInt(4, 8) * (float) Math.cos(System.currentTimeMillis() / 55.0);

                return new Rotation(
                        MathHelper.lerp(0.85F, currentRotation.getYaw(), currentRotation.getYaw() + MathHelper.clamp(yawDiff, -maxStepYaw, maxStepYaw)) + shakeYaw,
                        MathHelper.lerp(0.85F, currentRotation.getPitch(), currentRotation.getPitch() + MathHelper.clamp(pitchDiff, -maxStepPitch, maxStepPitch)) + shakePitch
                );
            } else {
                Rotation playerRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
                Rotation diffToPlayer = RotationUtil.calculateDelta(currentRotation, playerRotation);

                float yawDiff = diffToPlayer.getYaw();
                float pitchDiff = diffToPlayer.getPitch();
                float rotationDistance = calculateRotationDistance(yawDiff, pitchDiff);

                float shakeYaw = randomInt(8, 11) * (float) Math.sin(System.currentTimeMillis() / 55.0);
                float shakePitch = randomInt(4, 8) * (float) Math.cos(System.currentTimeMillis() / 55.0);

                if (aura.isEnabled() && aura.target != null) {
                    this.smoothbackShakeStartMs = -1L;
                } else {
                    if (this.smoothbackShakeStartMs < 0L) {
                        this.smoothbackShakeStartMs = System.currentTimeMillis();
                    }

                    float shakeFactor = 1.0F - MathHelper.clamp((float) (System.currentTimeMillis() - this.smoothbackShakeStartMs) / 1000.0F, 0.0F, 1.0F);
                    shakeYaw *= shakeFactor;
                    shakePitch *= shakeFactor;
                }

                float maxReturnYaw = Math.abs(yawDiff / rotationDistance) * 45.0F;
                float maxReturnPitch = Math.abs(pitchDiff / rotationDistance) * 45.0F;

                return new Rotation(
                        MathHelper.lerp(0.85F, currentRotation.getYaw(), currentRotation.getYaw() + MathHelper.clamp(yawDiff, -maxReturnYaw, maxReturnYaw) + shakeYaw),
                        MathHelper.lerp(0.85F, currentRotation.getPitch(), currentRotation.getPitch() + MathHelper.clamp(pitchDiff, -maxReturnPitch, maxReturnPitch) + shakePitch)
                );
            }
        } else {
            return currentRotation;
        }
    }

    private float calculateRotationDistance(float yawDiff, float pitchDiff) {
        float distance = (float) Math.hypot(Math.abs(yawDiff), Math.abs(pitchDiff));
        return distance < 0.01f ? 1f : distance;
    }

    private int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}

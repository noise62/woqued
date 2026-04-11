package sweetie.evaware.api.utils.rotation.rotations;

import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.neuro.AIPredictor;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;

public class NeuroRotation extends RotationMode {
    private final AIPredictor predictor;
    private final float yawSpeed;
    private final float pitchSpeed;

    public NeuroRotation(AIPredictor predictor, float yawSpeed, float pitchSpeed) {
        super("Sloth");
        this.predictor = predictor;
        this.yawSpeed = yawSpeed;
        this.pitchSpeed = pitchSpeed;
    }

    Rotation aiRotation = Rotation.DEFAULT;
    Rotation prevRotation = Rotation.DEFAULT;

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        prevRotation = aiRotation;

        if (entity == null || mc.player == null) {
            float y = 40f / 3f;
            float p = 20f / 3f;
            Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
            return new Rotation(
                    currentRotation.getYaw() + MathHelper.clamp(delta.getYaw(), -y, y),
                    currentRotation.getPitch() + MathHelper.clamp(delta.getPitch(), -p, p)
            );
        }

        aiRotation = predictor.predict(entity, aiRotation, prevRotation, new Vector2f(100 - yawSpeed, 100 - pitchSpeed));

        return aiRotation;
    }

    public float sinWave(double value, double delayMS, Easing easing) {
        return (float) (MathHelper.clamp(easing.apply((Math.sin(System.currentTimeMillis() / delayMS) + 1F) / 2F), 0f, 1f) * value);
    }

    public float cosWave(double value, double delayMS, Easing easing) {
        return (float) (MathHelper.clamp(easing.apply((Math.cos(System.currentTimeMillis() / delayMS) + 1F) / 2F), 0f, 1f) * value);
    }

    public float sinWave(double from, double to, double delayMS, Easing easing) {
        return (float) (from + sinWave(to, delayMS, easing));
    }

    public float cosWave(double from, double to, double delayMS, Easing easing) {
        return (float) (from + cosWave(to, delayMS, easing));
    }

    public float calc(float input, float target, double step) {
        return (float) (input + step * (target - input));
    }
}
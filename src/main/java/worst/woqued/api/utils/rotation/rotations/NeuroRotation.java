package worst.woqued.client.features.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.api.system.backend.SharedClass;

public class NeuroRotation extends RotationMode {

    private final NeuroRotationManager neuroManager;
    private Rotation lastAppliedRotation;

    public NeuroRotation() {
        super("Neuro");
        this.neuroManager = NeuroRotationManager.getInstance();
        this.lastAppliedRotation = null;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (!neuroManager.isPlaying() || SharedClass.player() == null) {
            lastAppliedRotation = null;
            return targetRotation;
        }

        AuraModule aura = AuraModule.getInstance();
        if (!aura.isEnabled()) {
            lastAppliedRotation = null;
            return targetRotation;
        }

        Rotation neuroRotation = neuroManager.getNextRotation();
        if (neuroRotation != null) {
            if (lastAppliedRotation == null) {
                lastAppliedRotation = currentRotation;
            }

            float smoothFactor = 0.08f;
            float smoothedYaw = MathHelper.lerp(smoothFactor, lastAppliedRotation.getYaw(), neuroRotation.getYaw());
            float smoothedPitch = MathHelper.lerp(smoothFactor, lastAppliedRotation.getPitch(), neuroRotation.getPitch());

            smoothedYaw = MathHelper.wrapDegrees(smoothedYaw);
            smoothedPitch = MathHelper.clamp(smoothedPitch, -90f, 90f);

            lastAppliedRotation = new Rotation(smoothedYaw, smoothedPitch);
            return lastAppliedRotation;
        }

        lastAppliedRotation = null;
        return targetRotation;
    }
}
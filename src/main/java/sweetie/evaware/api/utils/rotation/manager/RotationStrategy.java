package sweetie.evaware.api.utils.rotation.manager;

import sweetie.evaware.api.module.Module;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.rotation.rotations.SnapRotation;

@Setter
@Accessors(chain = true, fluent = true)
public class RotationStrategy {
    private final RotationMode rotationMode;
    private final boolean moveCorrection;
    private final boolean freeCorrection;

    private final float resetThreshold = 2f;
    private int ticksUntilReset = 5;

    private boolean clientLook;

    public static final RotationStrategy SMOOTH_FREE = new RotationStrategy(new SnapRotation(), true, true);
    public static final RotationStrategy SMOOTH_FOCUS = new RotationStrategy(new SnapRotation(), true);
    public static final RotationStrategy TARGET = new RotationStrategy(new SnapRotation(), true);

    public RotationStrategy(RotationMode rotationMode, boolean moveCorrection) {
        this.rotationMode = rotationMode;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = false;
    }

    public RotationStrategy(RotationMode rotationMode, boolean moveCorrection, boolean freeCorrection) {
        this.rotationMode = rotationMode;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = freeCorrection;
    }

    public RotationStrategy(boolean moveCorrection) {
        this(new SnapRotation(), moveCorrection);
    }

    public RotationStrategy() {
        this(new SnapRotation(), true, true);
    }

    public RotationPlan createRotationPlan(Rotation rotation, Vec3d vec, Entity entity, Module provider) {
        return new RotationPlan(rotation, vec, entity, rotationMode, ticksUntilReset, resetThreshold, moveCorrection, freeCorrection, provider).clientLook(clientLook);
    }

    public RotationPlan createRotationPlan(Rotation rotation, Module provider) {
        return new RotationPlan(rotation, null, null, rotationMode, ticksUntilReset, resetThreshold, moveCorrection, freeCorrection, provider).clientLook(clientLook);
    }

    public RotationPlan createRotationPlan(Rotation rotation, Vec3d vec, Entity entity, boolean moveCorrection, boolean freeCorrection, Module provider) {
        return new RotationPlan(rotation, vec, entity, rotationMode, ticksUntilReset, resetThreshold, moveCorrection, freeCorrection, provider).clientLook(clientLook);
    }
}

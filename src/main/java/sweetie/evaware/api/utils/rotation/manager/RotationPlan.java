package sweetie.evaware.api.utils.rotation.manager;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.rotation.RotationUtil;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class RotationPlan implements QuickImports {
    private final Rotation rotation;
    private final Vec3d vec3d;
    private final Entity entity;
    private final RotationMode rotationMode;
    private final int ticksUntilReset;
    private final float resetThreshold;
    private final boolean moveCorrection;
    private boolean freeMoveCorrection;

    private boolean clientLook;

    private final Module provider;

    public RotationPlan(Rotation rotation, Vec3d vec3d, Entity entity, RotationMode rotationMode, int ticksUntilReset, float resetThreshold, boolean moveCorrection, boolean freeMoveCorrection, Module provider) {
        this.rotation = rotation;
        this.vec3d = vec3d;
        this.entity = entity;
        this.rotationMode = rotationMode;
        this.ticksUntilReset = ticksUntilReset;
        this.resetThreshold = resetThreshold;
        this.moveCorrection = moveCorrection;
        this.freeMoveCorrection = freeMoveCorrection;
        this.provider = provider;
    }

    public Rotation nextRotation(Rotation fromRotation, boolean isResetting) {
        if (isResetting) {
            return rotationMode.process(fromRotation, RotationUtil.fromVec2f(mc.player.getRotationClient()));
        } else {
            return rotationMode.process(fromRotation, rotation, vec3d, entity);
        }
    }
}

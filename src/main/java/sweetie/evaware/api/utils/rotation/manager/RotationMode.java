package sweetie.evaware.api.utils.rotation.manager;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.math.MouseUtil;

@Getter
public abstract class RotationMode implements QuickImports, ModeSetting.NamedChoice {
    private final String name;

    public RotationMode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public Rotation process(Rotation currentRotation, Rotation targetRotation) {
        return process(currentRotation, targetRotation, null, null);
    }

    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d) {
        return process(currentRotation, targetRotation, vec3d, null);
    }

    public abstract Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity);
}

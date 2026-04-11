package sweetie.evaware.client.features.modules.combat.elytratarget;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.module.setting.ModeSetting;

import java.util.function.Function;

public enum TargetPosition implements ModeSetting.NamedChoice {
    EYES("Eyes", target -> target.getEyePos()),
    CENTER("Center", target -> target.getPos().add(0.0, target.getHeight() / 2.0, 0.0));

    private final String name;
    private final Function<LivingEntity, Vec3d> position;

    TargetPosition(String name, Function<LivingEntity, Vec3d> position) {
        this.name = name;
        this.position = position;
    }

    public Vec3d getPosition(LivingEntity target) {
        return position.apply(target);
    }

    @Override
    public String getName() {
        return name;
    }
}

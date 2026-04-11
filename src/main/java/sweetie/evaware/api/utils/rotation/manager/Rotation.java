package sweetie.evaware.api.utils.rotation.manager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;

@Getter
@Setter
public class Rotation {
    private float yaw;
    private float pitch;

    public static final Rotation DEFAULT = new Rotation(0f, 0f);

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation adjustSensitivity() {
        double gcd = MouseUtil.getGCD();
        Rotation previousRotation = RotationManager.getInstance().getServerRotation();

        float adjustedYaw = adjustAxis(yaw, previousRotation.yaw, gcd);
        float adjustedPitch = adjustAxis(pitch, previousRotation.pitch, gcd);

        return new Rotation(adjustedYaw, MathHelper.clamp(adjustedPitch, -90f, 90f));
    }

    private float adjustAxis(float axisValue, float previousValue, double gcd) {
        float delta = axisValue - previousValue;
        return previousValue + Math.round(delta / gcd) * (float) gcd;
    }

    public Vec3d getVector() {
        float f = pitch * 0.017453292f;
        float g = -yaw * 0.017453292f;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public Rotation rotationDeltaTo(Rotation targetRotation) {
        return RotationUtil.calculateDelta(this, targetRotation);
    }

    @Override
    public String toString() {
        return "Rotation(yaw=" + yaw + ", pitch=" + pitch + ")";
    }

    public record VecRotation(Rotation rotation, Vec3d vec) {
        @Override
        public String toString() {
            return "VecRotation(rotation=" + rotation + ", vec=" + vec + ")";
        }
    }
}

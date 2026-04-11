package sweetie.evaware.api.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import sweetie.evaware.api.system.interfaces.QuickImports;

@UtilityClass
public class ProjectionUtil implements QuickImports {
    private float previousSpeed = 0.0f;
    private float horizontalSpeed = 0.0f;

    public Vector2f project(@NotNull Vec3d vec3d) {
        return project(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public Vector2f project(double x, double y, double z) {
        var camera = mc.getEntityRenderDispatcher().camera;
        var cameraPos = camera.getPos();

        var yawQuat = RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw());
        var pitchQuat = RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch());

        var cameraRotation = yawQuat.mul(pitchQuat, new Quaternionf());
        cameraRotation = cameraRotation.conjugate(new Quaternionf());

        var result3f = new Vector3f(
                (float)(cameraPos.getX() - x),
                (float)(cameraPos.getY() - y),
                (float)(cameraPos.getZ() - z)
        );

        result3f.rotate(cameraRotation);

        if (mc.options.getBobView().getValue()) {
            if (mc.getCameraEntity() instanceof AbstractClientPlayerEntity p) {
                calculateViewBobbing((AbstractClientPlayerEntity) mc.getCameraEntity(), result3f);
            }
        }

        float fov = mc.gameRenderer.getFov(camera, mc.getRenderTickCounter().getTickDelta(false), true);

        return calculateScreenPosition(result3f, fov);
    }

    private void calculateViewBobbing(AbstractClientPlayerEntity player, Vector3f result3f) {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);

        float distanceMoved = player.distanceMoved;
        float lastDistanceMoved = player.lastDistanceMoved;
        float distanceDelta = distanceMoved - lastDistanceMoved;
        float g = -(distanceMoved + distanceDelta * tickDelta);
        float strideDistance = MathHelper.lerp(tickDelta, player.prevStrideDistance, player.strideDistance);

        float translateX = MathHelper.sin(g * (float)Math.PI) * strideDistance * 0.5F;
        float translateY = -Math.abs(MathHelper.cos(g * (float)Math.PI) * strideDistance);
        float rotateZ = MathHelper.sin(g * (float)Math.PI) * strideDistance * 3.0F;
        float rotateX = Math.abs(MathHelper.cos(g * (float)Math.PI - 0.2F) * strideDistance) * 5.0F;

        float rotateZRad = rotateZ * ((float)Math.PI / 180f);
        float rotateXRad = rotateX * ((float)Math.PI / 180f);

        if (rotateZRad != 0) {
            Quaternionf zRot = new Quaternionf().setAngleAxis(rotateZRad, 0.0f, 0.0f, 1.0f).conjugate();
            result3f.rotate(zRot);
        }

        if (rotateXRad != 0) {
            Quaternionf xRot = new Quaternionf().setAngleAxis(rotateXRad, 1.0f, 0.0f, 0.0f).conjugate();
            result3f.rotate(xRot);
        }

        result3f.add(translateX, -translateY, 0.0f);
    }

    private Vector2f calculateScreenPosition(Vector3f result3f, double fov) {
        float width = mc.getWindow().getScaledWidth() / 2.0f;
        float height = mc.getWindow().getScaledHeight() / 2.0f;
        float x = result3f.x;
        float y = result3f.y;
        float z = result3f.z;

        double scaleFactor = height / (z * Math.tan(Math.toRadians(fov / 2.0)));
        return (z < 0.0f)
                ? new Vector2f((float) (-x * scaleFactor + width), (float) (height - y * scaleFactor))
                : new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
    }
}

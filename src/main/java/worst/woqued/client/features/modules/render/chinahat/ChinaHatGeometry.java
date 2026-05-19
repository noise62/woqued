package worst.woqued.client.features.modules.render.chinahat;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChinaHatGeometry {
    private final Map<MeshProfile, ChinaHatMesh> cache = new HashMap<>();

    public ChinaHatMesh getMesh(MeshProfile profile) {
        return cache.computeIfAbsent(profile, this::buildMesh);
    }

    private ChinaHatMesh buildMesh(MeshProfile profile) {
        List<Triangle> triangles = new ArrayList<>();
        List<LineLoop> loops = new ArrayList<>();
        List<LineSegment> spokes = new ArrayList<>();

        MeshVertex[][] rings = new MeshVertex[profile.countY()][profile.countX() + 1];
        MeshVertex top = new MeshVertex(0f, profile.hatHeight(), 0f, 0.5f, 0.5f, 0f);

        for (int sliceIndex = 1; sliceIndex <= profile.countY(); sliceIndex++) {
            float slicePc = sliceIndex / (float) profile.countY();
            float radius = slicePc * profile.hatWidth();
            float y = layerYOffset(slicePc, profile.hatHeight());
            MeshVertex[] ring = new MeshVertex[profile.countX() + 1];

            for (int pointIndex = 0; pointIndex <= profile.countX(); pointIndex++) {
                float anglePc = pointIndex / (float) profile.countX();
                float angle = anglePc * MathHelper.TAU;
                float sin = MathHelper.sin(angle);
                float cos = MathHelper.cos(angle);
                float px = sin * radius;
                float pz = cos * radius;
                float u = 0.5f + sin * slicePc * 0.5f;
                float v = 0.5f + cos * slicePc * 0.5f;
                ring[pointIndex] = new MeshVertex(px, y, pz, u, v, slicePc);
            }

            rings[sliceIndex - 1] = ring;
            loops.add(new LineLoop(List.of(ring)));
        }

        MeshVertex[] firstRing = rings[0];
        for (int pointIndex = 0; pointIndex < profile.countX(); pointIndex++) {
            triangles.add(new Triangle(top, firstRing[pointIndex], firstRing[pointIndex + 1]));
        }

        for (int sliceIndex = 0; sliceIndex < profile.countY() - 1; sliceIndex++) {
            MeshVertex[] upper = rings[sliceIndex];
            MeshVertex[] lower = rings[sliceIndex + 1];
            for (int pointIndex = 0; pointIndex < profile.countX(); pointIndex++) {
                MeshVertex upperLeft = upper[pointIndex];
                MeshVertex upperRight = upper[pointIndex + 1];
                MeshVertex lowerLeft = lower[pointIndex];
                MeshVertex lowerRight = lower[pointIndex + 1];

                triangles.add(new Triangle(upperLeft, lowerLeft, lowerRight));
                triangles.add(new Triangle(upperLeft, lowerRight, upperRight));
            }
        }

        int spokeStep = Math.max(1, profile.countX() / 15);
        MeshVertex[] lastRing = rings[profile.countY() - 1];
        for (int pointIndex = 0; pointIndex < profile.countX(); pointIndex += spokeStep) {
            spokes.add(new LineSegment(top, lastRing[pointIndex]));
        }

        return new ChinaHatMesh(triangles, loops, spokes);
    }

    private float layerYOffset(float slicePc, float hatHeight) {
        float yOffset = (1f - slicePc) * hatHeight;
        yOffset -= yOffset / 2f * easeInOutExpo(valWave01(slicePc));
        return yOffset;
    }

    public static float valWave01(float value) {
        return MathHelper.sin(MathHelper.clamp(value, 0f, 1f) * MathHelper.PI);
    }

    public static float easeInOutExpo(float value) {
        if (value <= 0f) {
            return 0f;
        }
        if (value >= 1f) {
            return 1f;
        }
        if (value < 0.5f) {
            return (float) (Math.pow(2d, 20d * value - 10d) / 2d);
        }
        return (float) ((2d - Math.pow(2d, -20d * value + 10d)) / 2d);
    }

    public record MeshProfile(float hatWidth, float hatHeight, int countX, int countY) {
    }

    public record ChinaHatMesh(List<Triangle> triangles, List<LineLoop> loops, List<LineSegment> spokes) {
    }

    public record Triangle(MeshVertex first, MeshVertex second, MeshVertex third) {
    }

    public record LineLoop(List<MeshVertex> vertices) {
    }

    public record LineSegment(MeshVertex start, MeshVertex end) {
    }

    public record MeshVertex(float x, float y, float z, float u, float v, float slicePc) {
    }
}

package sweetie.evaware.api.utils.math;

public class NoiseUtil {
    private static final int PERMUTATION_SIZE = 256;
    private final int[] permutation = new int[PERMUTATION_SIZE * 2];

    public NoiseUtil() {
        this(System.nanoTime());
    }

    public NoiseUtil(long seed) {
        RandomUtil randomUtil = new RandomUtil(seed);
        int[] p = new int[PERMUTATION_SIZE];
        for (int i = 0; i < PERMUTATION_SIZE; i++) {
            p[i] = i;
        }

        for (int i = PERMUTATION_SIZE - 1; i > 0; i--) {
            int j = randomUtil.nextInt(i + 1);
            int temp = p[i];
            p[i] = p[j];
            p[j] = temp;
        }

        for (int i = 0; i < PERMUTATION_SIZE * 2; i++) {
            permutation[i] = p[i & 255];
        }
    }

    public float perlinNoise(float x) {
        int xi = (int) Math.floor(x) & 255;
        float xf = x - (float) Math.floor(x);

        float u = fade(xf);

        int a = permutation[xi];
        int b = permutation[xi + 1];

        return lerp(u, grad(a, xf), grad(b, xf - 1)) * 2;
    }

    public float perlinNoise(float x, float y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;

        float xf = x - (float) Math.floor(x);
        float yf = y - (float) Math.floor(y);

        float u = fade(xf);
        float v = fade(yf);

        int aa = permutation[permutation[xi] + yi];
        int ab = permutation[permutation[xi] + yi + 1];
        int ba = permutation[permutation[xi + 1] + yi];
        int bb = permutation[permutation[xi + 1] + yi + 1];

        float x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1, yf));
        float x2 = lerp(u, grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1));

        return lerp(v, x1, x2);
    }

    public float perlinNoise(float x, float y, float z) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        int zi = (int) Math.floor(z) & 255;

        float xf = x - (float) Math.floor(x);
        float yf = y - (float) Math.floor(y);
        float zf = z - (float) Math.floor(z);

        float u = fade(xf);
        float v = fade(yf);
        float w = fade(zf);

        int aaa = permutation[permutation[permutation[xi] + yi] + zi];
        int aab = permutation[permutation[permutation[xi] + yi] + zi + 1];
        int aba = permutation[permutation[permutation[xi] + yi + 1] + zi];
        int abb = permutation[permutation[permutation[xi] + yi + 1] + zi + 1];
        int baa = permutation[permutation[permutation[xi + 1] + yi] + zi];
        int bab = permutation[permutation[permutation[xi + 1] + yi] + zi + 1];
        int bba = permutation[permutation[permutation[xi + 1] + yi + 1] + zi];
        int bbb = permutation[permutation[permutation[xi + 1] + yi + 1] + zi + 1];

        float x1 = lerp(u, grad(aaa, xf, yf, zf), grad(baa, xf - 1, yf, zf));
        float x2 = lerp(u, grad(aba, xf, yf - 1, zf), grad(bba, xf - 1, yf - 1, zf));
        float y1 = lerp(v, x1, x2);

        x1 = lerp(u, grad(aab, xf, yf, zf - 1), grad(bab, xf - 1, yf, zf - 1));
        x2 = lerp(u, grad(abb, xf, yf - 1, zf - 1), grad(bbb, xf - 1, yf - 1, zf - 1));
        float y2 = lerp(v, x1, x2);

        return lerp(w, y1, y2);
    }

    private float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private float lerp(float amount, float left, float right) {
        return left + amount * (right - left);
    }

    private float grad(int hash, float x) {
        return (hash & 1) == 0 ? x : -x;
    }

    private float grad(int hash, float x, float y) {
        switch (hash & 3) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            default: return 0;
        }
    }

    private float grad(int hash, float x, float y, float z) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public float octaveNoise(float x, float y, int octaves, float persistence, float lacunarity) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += perlinNoise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return total / maxValue;
    }
}
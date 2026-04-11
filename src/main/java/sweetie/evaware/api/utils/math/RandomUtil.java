package sweetie.evaware.api.utils.math;

import java.util.Random;

public class RandomUtil extends Random {
    private long seed;

    public RandomUtil(long seed) {
        this.seed = seed;
    }

    public RandomUtil() {
        this(System.nanoTime());
    }

    public int randomInRange(int min, int max) {
        if (min == max || min > max) return min;
        return nextInt(max - min + 1) + min;
    }

    public float randomInRange(float min, float max) {
        //if (min == max || min > max) return min;
        return min + (nextFloat() * (max - min));
    }

    public double randomInRange(double min, double max) {
        //if (min == max || min > max) return min;
        return min + (nextDouble() * (max - min));
    }

    @Override
    protected int next(int bits) {
        seed ^= (seed << 13);
        seed ^= (seed >>> 17);
        seed ^= (seed << 5);
        return (int) (seed & ((1L << bits) - 1));
    }
}

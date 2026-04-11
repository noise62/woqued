package sweetie.evaware.api.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import sweetie.evaware.api.system.interfaces.QuickImports;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class MathUtil implements QuickImports {
    private final RandomUtil randomUtil = new RandomUtil();

    public double getEntityBPS(Entity entity) {
        return Math.hypot(entity.prevX - entity.getX(), entity.prevZ - entity.getZ()) * 20;
    }

    public float interpolate(double oldValue, double newValue) {
        return (float) (oldValue + (newValue - oldValue) * mc.getRenderTickCounter().getTickDelta(false));
    }

    public double interpolate(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    public float interpolate(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    public int interpolate(int oldValue, int newValue, float interpolationValue) {
        return (int)(oldValue + (newValue - oldValue) * interpolationValue);
    }

    public double round(double value, double step) {
        double v = (double) Math.round(value / step) * step;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public float round(float value, float step) {
        double v = (double) Math.round(value / step) * step;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public int randomInRange(int min, int max) {
        return randomUtil.randomInRange(min, max);
    }

    public float randomInRange(float min, float max) {
        return randomUtil.randomInRange(min, max);
    }

    public double randomInRange(double min, double max) {
        return randomUtil.randomInRange(min, max);
    }
}
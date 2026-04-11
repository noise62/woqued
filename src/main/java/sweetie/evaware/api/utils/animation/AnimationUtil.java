package sweetie.evaware.api.utils.animation;

import lombok.Getter;
import lombok.Setter;
import sweetie.evaware.api.utils.math.MathUtil;

public class AnimationUtil {
    private long start = 0L;
    @Setter private long duration = 0L;
    @Getter private double fromValue = 0.0;
    @Getter private double toValue = 0.0;
    @Setter @Getter private double value = 0.0;
    @Setter private Easing easing = Easing.LINEAR;

    public AnimationUtil run(float valueTo, long duration, Easing easing) {
        return run((double) valueTo, duration, easing, false);
    }

    public AnimationUtil run(double valueTo, long duration, Easing easing) {
        return run(valueTo, duration, easing, false);
    }

    public AnimationUtil run(double valueTo, long duration, Easing easing, boolean safe) {
        if (check(safe, valueTo)) {
        } else {
            this.easing = easing;
            this.duration = duration;
            this.start = System.currentTimeMillis();
            this.fromValue = value;
            this.toValue = valueTo;
        }
        return this;
    }

    public boolean update() {
        boolean alive = isAlive();
        if (alive) {
            value = MathUtil.interpolate(fromValue, toValue, easing.apply(calculatePart()));
        } else {
            start = 0L;
            value = toValue;
        }
        return alive;
    }

    public boolean isAlive() {
        return !isFinished();
    }

    public boolean isFinished() {
        return calculatePart() >= 1.0;
    }

    private double calculatePart() {
        return (System.currentTimeMillis() - start) / (double) duration;
    }

    private boolean check(boolean safe, double valueTo) {
        return safe && isAlive() && (valueTo == fromValue || valueTo == toValue || valueTo == value);
    }
}

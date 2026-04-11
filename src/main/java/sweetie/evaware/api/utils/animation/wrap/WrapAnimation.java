package sweetie.evaware.api.utils.animation.wrap;

import lombok.Getter;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.math.TimerUtil;

@Getter
public class WrapAnimation {
	private final TimerUtil timer = new TimerUtil();
    private int speed;
    private double size = 1;
    @Getter
    private boolean forward;
    private Easing easing;

    public boolean finished(boolean forward) {
        return timer.finished(speed) && (forward ? this.forward : !this.forward);
    }
    
    public boolean finished() {
        return timer.finished(speed) && this.forward;
    }
    
    public WrapAnimation setForward(boolean forward) {
    	if (this.forward != forward) {
    		this.forward = forward;
        	timer.setMillis((long) (System.currentTimeMillis() - (size - Math.min(size, timer.getElapsedTime()))));
    	}
    	return this;
    }
    
    public WrapAnimation finish() {
    	timer.setMillis((long) (System.currentTimeMillis()-speed));
    	return this;
    }
    
    public WrapAnimation setEasing(Easing easing) {
    	this.easing = easing;
    	return this;
    }
    
    public WrapAnimation setSpeed(int speed) {
    	this.speed = speed;
    	return this;
    }
    
    public WrapAnimation setSize(float size) {
    	this.size = size;
    	return this;
    }
    
    public float getLinear() {
    	if (forward) {
            if (timer.finished(speed)) {
                return (float) size;
            }

            return (float) (timer.getElapsedTime() / (double) speed * size);
        } else {
            if (timer.finished(speed)) {
                return 0.0f;
            }

            return (float) ((1 - timer.getElapsedTime() / (double) speed) * size);
        }
    }
    
    public float get() {
    	if (forward) {
            if (timer.finished(speed)) {
                return (float) size;
            }

            return (float) (easing.apply(timer.getElapsedTime() / (double) speed) * size);
        } else {
            if (timer.finished(speed)) {
                return 0.0f;
            }

            return (float) ((1 - easing.apply(timer.getElapsedTime() / (double) speed)) * size);
        }
    }
    
    public float reversed() {
    	return 1-get();
    }
    
    public void reset() {
    	timer.reset();
    }
    
    public void kuni() {
    	if (finished())
    		setForward(false);
    	else if (finished(false))
    		setForward(true);
    }
    
}

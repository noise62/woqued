package sweetie.evaware.api.utils.animation.wrap.infinity;

import lombok.Getter;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.animation.wrap.WrapAnimation;

public class InfinityAnimation {
    
	private float output, endpoint;
	private Easing easing = Easing.LINEAR;

    @Getter
    private WrapAnimation animation = new WrapAnimation().setSize(0).setSpeed(0).setForward(false).setEasing(easing);

    public float animate(float destination, int ms) {
    	ms = Math.max(1, ms);
        output = endpoint - animation.get();
        endpoint = destination;
        if (output != (endpoint - destination)) {
            animation = new WrapAnimation().setSize(endpoint - output).setSpeed(ms).setForward(false).setEasing(easing);
        }
        return output;
    }

    public boolean finished() {
        return output == endpoint || animation.finished() || animation.finished(false);
    }

    public float get() {
        output = endpoint - animation.get();
        return output;
    }
    
    public InfinityAnimation easing(Easing easing) {
		this.easing = easing;
		
		return this;
	}
    
}

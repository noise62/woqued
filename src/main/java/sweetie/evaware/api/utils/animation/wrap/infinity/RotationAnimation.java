package sweetie.evaware.api.utils.animation.wrap.infinity;

import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.rotation.manager.Rotation;

public class RotationAnimation {
	   
	private final InfinityAnimation yaw = new InfinityAnimation();
	private final InfinityAnimation pitch = new InfinityAnimation();
	
    public Rotation animate(Rotation rotation, int yawSpeed, int pitchSpeed) {
        return new Rotation(
        		yaw.animate(rotation.getYaw(), yawSpeed),
        		pitch.animate(rotation.getPitch(), pitchSpeed)
        );
    }
    
    public float getYaw() {
    	return this.yaw.get();
    }
    
    public float getPitch() {
    	return this.pitch.get();
    }
    
    public RotationAnimation easing(Easing easing) {
    	yaw.easing(easing);
    	yaw.easing(easing);
		
		return this;
	}
    

}

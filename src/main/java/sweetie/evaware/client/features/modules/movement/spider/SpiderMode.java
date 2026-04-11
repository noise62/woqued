package sweetie.evaware.client.features.modules.movement.spider;

import sweetie.evaware.api.event.events.player.move.MotionEvent;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.api.system.backend.Configurable;

public abstract class SpiderMode extends Choice {
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    public boolean hozColl() {
        return mc.player.horizontalCollision;
    }
}

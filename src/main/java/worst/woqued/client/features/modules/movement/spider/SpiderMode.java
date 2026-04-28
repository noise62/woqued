package worst.woqued.client.features.modules.movement.spider;

import worst.woqued.api.event.events.player.move.MotionEvent;
import worst.woqued.api.system.backend.Choice;

public abstract class SpiderMode extends Choice {
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    public boolean hozColl() {
        return mc.player.horizontalCollision;
    }
}

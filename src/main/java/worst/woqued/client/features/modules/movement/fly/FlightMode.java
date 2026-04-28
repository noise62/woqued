package worst.woqued.client.features.modules.movement.fly;

import worst.woqued.api.event.events.player.move.MotionEvent;
import worst.woqued.api.system.backend.Choice;

public abstract class FlightMode extends Choice {


    // events
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    // module methods
    public void onEnable() {}
    public void onDisable() {}
    public void toggle() {}
}

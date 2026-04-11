package sweetie.evaware.client.features.modules.movement.fly;

import sweetie.evaware.api.event.events.player.move.MotionEvent;
import sweetie.evaware.api.system.backend.Choice;

public abstract class FlightMode extends Choice {


    // events
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    // module methods
    public void onEnable() {}
    public void onDisable() {}
    public void toggle() {}
}

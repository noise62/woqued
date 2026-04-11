package sweetie.evaware.api.event.events.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sweetie.evaware.api.event.events.Event;

public class MotionEvent extends Event<MotionEvent.MotionEventData> {
    @Getter private static final MotionEvent instance = new MotionEvent();

    @Getter
    @Setter
    @Accessors(fluent = true)
    @AllArgsConstructor
    public static class MotionEventData {
        private double x, y, z, yaw, pitch;
        private boolean ground;
    }
}

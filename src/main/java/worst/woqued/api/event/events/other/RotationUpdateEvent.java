package worst.woqued.api.event.events.other;

import lombok.Getter;
import worst.woqued.api.event.events.Event;

public class RotationUpdateEvent extends Event<RotationUpdateEvent> {
    @Getter private static final RotationUpdateEvent instance = new RotationUpdateEvent();
}

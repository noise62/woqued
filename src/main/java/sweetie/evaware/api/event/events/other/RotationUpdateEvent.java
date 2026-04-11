package sweetie.evaware.api.event.events.other;

import lombok.Getter;
import sweetie.evaware.api.event.events.Event;

public class RotationUpdateEvent extends Event<RotationUpdateEvent> {
    @Getter private static final RotationUpdateEvent instance = new RotationUpdateEvent();
}

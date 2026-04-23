package worst.woqued.api.event.events.client;

import lombok.Getter;
import worst.woqued.api.event.events.Event;

public class TickEvent extends Event<TickEvent> {
    @Getter private static final TickEvent instance = new TickEvent();
}

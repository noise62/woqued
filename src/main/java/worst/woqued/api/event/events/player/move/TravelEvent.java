package worst.woqued.api.event.events.player.move;

import lombok.Getter;
import worst.woqued.api.event.events.Event;

public class TravelEvent extends Event<TravelEvent> {
    @Getter private static final TravelEvent instance = new TravelEvent();
}

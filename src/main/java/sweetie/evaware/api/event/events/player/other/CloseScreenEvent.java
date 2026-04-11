package sweetie.evaware.api.event.events.player.other;

import lombok.Getter;
import sweetie.evaware.api.event.events.Event;

public class CloseScreenEvent extends Event<CloseScreenEvent> {
    @Getter private static final CloseScreenEvent instance = new CloseScreenEvent();
}

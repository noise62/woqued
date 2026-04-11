package sweetie.evaware.api.event.events.client;

import lombok.Getter;
import sweetie.evaware.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
    @Getter private static final GameLoopEvent instance = new GameLoopEvent();
}

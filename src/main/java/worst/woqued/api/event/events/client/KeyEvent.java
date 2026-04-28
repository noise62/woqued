package worst.woqued.api.event.events.client;

import lombok.Getter;
import worst.woqued.api.event.events.Event;

public class KeyEvent extends Event<KeyEvent.KeyEventData> {
    @Getter private static final KeyEvent instance = new KeyEvent();

    public record KeyEventData(int key, int action, boolean mouse) { }
}

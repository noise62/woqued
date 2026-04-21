package worst.woqued.api.event.events.other;

import lombok.Getter;
import worst.woqued.api.event.events.Event;

public class WindowResizeEvent extends Event<WindowResizeEvent> {
    @Getter private static final WindowResizeEvent instance = new WindowResizeEvent();
}

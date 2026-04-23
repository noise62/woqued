package worst.woqued.api.event.events.other;

import lombok.Getter;
import worst.woqued.api.event.events.Event;

public class FramebufferResizeEvent extends Event<FramebufferResizeEvent> {
    @Getter private static final FramebufferResizeEvent instance = new FramebufferResizeEvent();
}

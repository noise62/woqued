package sweetie.evaware.api.event.events.other;

import lombok.Getter;
import sweetie.evaware.api.event.events.Event;

public class FramebufferResizeEvent extends Event<FramebufferResizeEvent> {
    @Getter private static final FramebufferResizeEvent instance = new FramebufferResizeEvent();
}

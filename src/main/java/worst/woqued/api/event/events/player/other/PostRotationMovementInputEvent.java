package worst.woqued.api.event.events.player.other;

import lombok.Getter;
import worst.woqued.api.event.events.Event;

public class PostRotationMovementInputEvent extends Event<PostRotationMovementInputEvent> {
    @Getter private static final PostRotationMovementInputEvent instance = new PostRotationMovementInputEvent();
}

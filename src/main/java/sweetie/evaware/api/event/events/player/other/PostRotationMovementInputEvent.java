package sweetie.evaware.api.event.events.player.other;

import lombok.Getter;
import sweetie.evaware.api.event.events.Event;

public class PostRotationMovementInputEvent extends Event<PostRotationMovementInputEvent> {
    @Getter private static final PostRotationMovementInputEvent instance = new PostRotationMovementInputEvent();
}

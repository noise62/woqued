package sweetie.evaware.api.event.events.player.other;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.PlayerInput;
import sweetie.evaware.api.event.events.Event;
import sweetie.evaware.api.utils.player.DirectionalInput;

public class MovementInputEvent extends Event<MovementInputEvent.MovementInputEventData> {
    @Getter private static final MovementInputEvent instance = new MovementInputEvent();

    @Getter
    @AllArgsConstructor
    public static class MovementInputEventData {
        private final PlayerInput playerInput;

        @Setter
        private boolean jump, sneak;

        private DirectionalInput directionalInput;
    }
}

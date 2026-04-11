package sweetie.evaware.api.event.events.player.move;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import sweetie.evaware.api.event.events.Event;
import sweetie.evaware.api.utils.player.DirectionalInput;

public class SprintEvent extends Event<SprintEvent.SprintEventData> {
    @Getter private static final SprintEvent instance = new SprintEvent();

    @Override
    public boolean call(SprintEventData any) {
        any.setSprint(false);
        super.call(any);
        return any.isSprint();
    }

    @Setter
    @Getter
    @RequiredArgsConstructor
    public static class SprintEventData {
        private boolean sprint = false;

        private final DirectionalInput directionalInput;
    }
}

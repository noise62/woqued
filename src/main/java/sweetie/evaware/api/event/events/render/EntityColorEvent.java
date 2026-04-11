package sweetie.evaware.api.event.events.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sweetie.evaware.api.event.events.Event;

public class EntityColorEvent extends Event<EntityColorEvent.EntityColorEventData> {
    @Getter private static final EntityColorEvent instance = new EntityColorEvent();

    @Getter @Setter
    @Accessors(fluent = true)
    @AllArgsConstructor
    public static class EntityColorEventData {
        int color;
    }
}

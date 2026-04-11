package sweetie.evaware.api.event.events.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.events.Event;

public class VelocityEvent extends Event<VelocityEvent.VelocityEventData> {
    @Getter private static final VelocityEvent instance = new VelocityEvent();

    @Getter
    @AllArgsConstructor
    public static class VelocityEventData {
        @Setter private Vec3d movementInput;
        private float speed;
        private float yaw;

        @Setter private Vec3d velocity;
    }
}

package sweetie.evaware.api.event.events.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.events.Event;

public class MoveEvent extends Event<MoveEvent.MoveEventData> {
    @Getter private static final MoveEvent instance = new MoveEvent();

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MoveEventData {
        private double x;
        private double y;
        private double z;

        public void set(Vec3d vec3d) {
            x = vec3d.getX();
            y = vec3d.getY();
            z = vec3d.getZ();
        }
    }
}

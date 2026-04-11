package sweetie.evaware.api.event.events.render;

import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.event.events.Event;

public class Render3DEvent extends Event<Render3DEvent.Render3DEventData> {
    @Getter private static final Render3DEvent instance = new Render3DEvent();

    public record Render3DEventData(MatrixStack matrixStack, float partialTicks) { }
}

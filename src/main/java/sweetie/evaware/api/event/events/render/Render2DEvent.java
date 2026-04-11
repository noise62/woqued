package sweetie.evaware.api.event.events.render;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.event.events.Event;

public class Render2DEvent extends Event<Render2DEvent.Render2DEventData> {
    @Getter private static final Render2DEvent instance = new Render2DEvent();

    public record Render2DEventData(DrawContext context, MatrixStack matrixStack, float partialTicks) { }
}

package worst.woqued.client.ui.widget;

import lombok.Getter;
import lombok.Setter;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.api.system.draggable.Draggable;
import worst.woqued.api.system.draggable.DraggableManager;
import worst.woqued.api.system.interfaces.IRenderer;
import worst.woqued.api.system.interfaces.QuickImports;
import worst.woqued.api.utils.animation.Easing;
import worst.woqued.api.utils.render.fonts.Font;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.features.modules.render.InterfaceModule;
import worst.woqued.client.services.RenderService;

@Getter
@Setter
public abstract class Widget implements QuickImports, IRenderer {
    protected Widget(float x, float y) {
        this.draggable = create(x, y, getName());
    }

    private final Easing easing = Easing.SINE_OUT;
    private final long duration = 100;

    public abstract String getName();
    private final Draggable draggable;
    private boolean enabled;

    private Draggable create(float x, float y, String name) {
        return DraggableManager.getInstance().create(InterfaceModule.getInstance(), name, x, y);
    }

    protected void setDraggableSize(int width, int height) {
        this.draggable.setWidth(width);
        this.draggable.setHeight(height);
    }

    public void render(Render2DEvent.Render2DEventData event) {
        render(event.matrixStack());
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    public float getScale() { return RenderService.getInstance().getScale(); }
    public float getGap() { return scaled(3f); }
    public Font getMediumFont() { return Fonts.PS_MEDIUM; }
    public Font getSemiBoldFont() { return Fonts.PS_BOLD; }
}

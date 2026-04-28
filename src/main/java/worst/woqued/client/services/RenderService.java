package worst.woqued.client.services;

import lombok.Getter;
import lombok.Setter;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.other.WindowResizeEvent;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.api.system.interfaces.QuickImports;
import worst.woqued.api.utils.math.MathUtil;
import worst.woqued.client.features.modules.render.InterfaceModule;

@Getter
public class RenderService implements QuickImports {
    @Getter private static final RenderService instance = new RenderService();

    @Setter private float scale = 1.0f;

    private final Listener<Render2DEvent.Render2DEventData> renderListener;

    public RenderService() {
        this.renderListener = new Listener<>(event -> {
            updateScale();
        });
    }

    public void load() {
        WindowResizeEvent.getInstance().subscribe(new Listener<>(event -> {
            register();
        }));
    }

    private void register() {
        Render2DEvent.getInstance().subscribe(renderListener);
    }

    public float scaled(float value) {
        return value * scale;
    }

    public void updateScale() {
        float w = mc.getWindow().getScaledWidth();
        float h = mc.getWindow().getScaledHeight();

        float bW = 1366f / 2f;
        float bH = 768f / 2f;

        float newScale = Math.max(w / bW, h / bH) * InterfaceModule.getScale();

        if (scale == newScale) {
            this.scale = newScale;
            Render2DEvent.getInstance().unsubscribe(renderListener);
            return;
        }

        scale = MathUtil.interpolate(scale, newScale, 0.15f);
    }
}
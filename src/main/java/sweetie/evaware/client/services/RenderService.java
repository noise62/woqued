package sweetie.evaware.client.services;

import lombok.Getter;
import lombok.Setter;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.other.WindowResizeEvent;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.client.features.modules.render.InterfaceModule;

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
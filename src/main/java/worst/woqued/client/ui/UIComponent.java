package worst.woqued.client.ui;

import lombok.Getter;
import lombok.Setter;
import worst.woqued.api.system.interfaces.UIApi;
import worst.woqued.client.services.RenderService;

@Getter
@Setter
public abstract class UIComponent implements UIApi {
    private float x, y, width, height, alpha;

    public float gap() {
        return scaled(2f);
    }

    public float offset() {
        return gap() * 1.5f;
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    public float getScale() {
        return RenderService.getInstance().getScale();
    }
}

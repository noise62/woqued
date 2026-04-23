package worst.woqued.client.ui.widget.overlay;

import worst.woqued.api.utils.math.MathUtil;
import worst.woqued.api.utils.render.fonts.Icons;
import worst.woqued.client.ui.widget.InformationWidget;

public class BPSWidget extends InformationWidget {
    @Override
    public String getName() {
        return "BPS";
    }

    public BPSWidget() {
        super(80f, 120f);
    }

    @Override
    public String getValue() {
        return String.format("%.2f", MathUtil.getEntityBPS(mc.player));
    }

    @Override
    public Icons getIcon() {
        return null;
    }
}

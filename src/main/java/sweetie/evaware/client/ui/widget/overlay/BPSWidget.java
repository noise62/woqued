package sweetie.evaware.client.ui.widget.overlay;

import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.fonts.Icons;
import sweetie.evaware.client.ui.widget.InformationWidget;

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

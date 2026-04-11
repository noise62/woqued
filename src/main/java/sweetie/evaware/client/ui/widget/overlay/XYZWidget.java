package sweetie.evaware.client.ui.widget.overlay;

import sweetie.evaware.api.utils.render.fonts.Icons;
import sweetie.evaware.client.ui.widget.InformationWidget;

public class XYZWidget extends InformationWidget {
    @Override
    public String getName() {
        return "XYZ";
    }

    public XYZWidget() {
        super(30f, 120f);
    }

    @Override
    public String getValue() {
        String x = String.format("%.1f", mc.player.getX());
        String y = String.format("%.1f", mc.player.getY());
        String z = String.format("%.1f", mc.player.getZ());
        return x + ", " + y + ", " + z;
    }

    @Override
    public Icons getIcon() {
        return null;
    }
}

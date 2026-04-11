package sweetie.evaware.client.ui.theme.basic;

import java.awt.*;

public class CandyLoveTheme extends ADefaultTheme {
    public CandyLoveTheme() {
        super("Candy Love");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 173, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(240, 127, 212, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(90, 58, 89, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(115, 71, 115, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(141, 90, 141, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 217, 250, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(207, 174, 194, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(240, 153, 254, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}

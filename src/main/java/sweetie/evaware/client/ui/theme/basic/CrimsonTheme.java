package sweetie.evaware.client.ui.theme.basic;

import java.awt.*;

public class CrimsonTheme extends ADefaultTheme {
    public CrimsonTheme() {
        super("Crimson");
    }

    @Override
    public Color setPrimary() {
        return new Color(227, 87, 87, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(143, 45, 45, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(16, 7, 7, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(48, 22, 22, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(62, 31, 31, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 202, 202, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(205, 136, 136, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(254, 128, 128, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}


package sweetie.evaware.client.ui.theme.basic;

import java.awt.*;

public class BlueTheme extends ADefaultTheme {
    public BlueTheme() {
        super("Lavender");
    }

    @Override
    public Color setPrimary() {
        return new Color(173, 176, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(136, 135, 202, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(42, 42, 61, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(64, 64, 101, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(58, 56, 92, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 255, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(187, 187, 187, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(180, 186, 232, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(255, 255, 255, 255);
    }
}

package sweetie.evaware.api.utils.color;

import lombok.experimental.UtilityClass;
import sweetie.evaware.client.ui.theme.Theme;
import sweetie.evaware.client.ui.theme.ThemeEditor;

import java.awt.*;

@UtilityClass
public class UIColors {
    public Theme currentTheme() {
        return ThemeEditor.getInstance().getCurrentTheme();
    }

    private Color getColor(Color color, int alpha) {
        int finalAlpha = (int) (color.getAlpha() / 255f * alpha);
        return ColorUtil.setAlpha(color, finalAlpha);
    }

    public Color gradient(int index) { return gradient(index, 255); }
    public Color gradient(int index, int alpha) { return getColor(ColorUtil.gradient(15, index, primary(alpha), secondary(alpha)), alpha); }

    public Color blur() { return blur(255); }
    public Color blur(int alpha) { return getColor(currentTheme().getBlurColor(), alpha); }

    public Color widgetBlur() { return widgetBlur(255); }
    public Color widgetBlur(int alpha) { return getColor(currentTheme().getWidgetBlurColor(), alpha); }

    public Color backgroundBlur() { return backgroundBlur(255); }
    public Color backgroundBlur(int alpha) { return getColor(currentTheme().getBackgroundBlurColor(), alpha); }

    public Color primary() { return primary(255); }
    public Color primary(int alpha) { return getColor(currentTheme().getPrimaryColor(), alpha); }

    public Color secondary() { return secondary(255); }
    public Color secondary(int alpha) { return getColor(currentTheme().getSecondaryColor(), alpha); }

    public Color knob() { return knob(255); }
    public Color knob(int alpha) { return getColor(currentTheme().getKnobColor(), alpha); }

    public Color inactiveKnob() { return inactiveKnob(255); }
    public Color inactiveKnob(int alpha) { return getColor(currentTheme().getInactiveKnobColor(), alpha); }

    public Color textColor() { return textColor(255); }
    public Color textColor(int alpha) { return getColor(currentTheme().getTextColor(), alpha); }

    public Color inactiveTextColor() { return inactiveTextColor(255); }
    public Color inactiveTextColor(int alpha) { return getColor(currentTheme().getInactiveTextColor(), alpha); }

    public Color positiveColor() { return positiveColor(255); }
    public Color positiveColor(int alpha) { return getColor(currentTheme().getPositiveColor(), alpha); }
    public Color middleColor() { return middleColor(255); }
    public Color middleColor(int alpha) { return getColor(currentTheme().getMiddleColor(), alpha); }
    public Color negativeColor() { return negativeColor(255); }
    public Color negativeColor(int alpha) { return getColor(currentTheme().getNegativeColor(), alpha); }
}
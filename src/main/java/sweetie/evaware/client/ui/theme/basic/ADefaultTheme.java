package sweetie.evaware.client.ui.theme.basic;

import sweetie.evaware.client.ui.theme.Theme;

import java.awt.*;

public abstract class ADefaultTheme extends Theme {
    public ADefaultTheme(String name) {
        super(name);
    }

    public abstract Color setPrimary();
    public abstract Color setSecondary();
    public abstract Color setBlur();
    public abstract Color setWidgetBlur();
    public abstract Color setBackgroundBlur();
    public abstract Color setText();
    public abstract Color setInactiveText();
    public abstract Color setKnob();
    public abstract Color setInactiveKnob();

    public ADefaultTheme update() {
        for (ElementColor elementColor : getElementColors()) {
            switch (elementColor.getName()) {
                case "Primary" -> elementColor.setColor(setPrimary());
                case "Secondary" -> elementColor.setColor(setSecondary());
                case "Blur" -> elementColor.setColor(setBlur());
                case "Widget blur" -> elementColor.setColor(setWidgetBlur());
                case "Background blur" -> elementColor.setColor(setBackgroundBlur());
                case "Text" -> elementColor.setColor(setText());
                case "Inactive text" -> elementColor.setColor(setInactiveText());
                case "Knob" -> elementColor.setColor(setKnob());
                case "Inactive knob" -> elementColor.setColor(setInactiveKnob());
            }
        }
        return this;
    }
}

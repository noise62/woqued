package sweetie.evaware.api.system.backend;

import lombok.experimental.UtilityClass;
import org.lwjgl.glfw.GLFW;
import sweetie.evaware.api.system.interfaces.QuickImports;

import java.lang.reflect.Field;

@UtilityClass
public class KeyStorage implements QuickImports {
    public boolean isPressed(int keyCode) {
        if (keyCode == -1 || keyCode == -999) return false;

        if (keyCode <= -100 + GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 100 + keyCode) == GLFW.GLFW_PRESS;
        }

        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    public int getBind(String s) {
        if (s == null || s.isEmpty()) return -1;
        if (s.equalsIgnoreCase("None")) return -1;

        if (s.startsWith("Mouse")) {
            try {
                int mouseIndex = Integer.parseInt(s.substring(5)) - 1;
                if (mouseIndex >= GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseIndex <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                    return -100 + mouseIndex;
                }
            } catch (NumberFormatException ignored) {}
            return -1;
        }

        try {
            for (Field field : GLFW.class.getDeclaredFields()) {
                if (field.getName().startsWith("GLFW_KEY_") && field.getType() == int.class) {
                    String fieldName = field.getName().substring("GLFW_KEY_".length());
                    if (formatKeyLabel(fieldName).equalsIgnoreCase(s) || fieldName.equalsIgnoreCase(s)) {
                        return field.getInt(null);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public String getBind(int key) {
        if (key == -1 || key == -999) return "None";

        for (int i = GLFW.GLFW_MOUSE_BUTTON_LEFT; i <= GLFW.GLFW_MOUSE_BUTTON_LAST; i++) {
            if (key == -100 + i) {
                return "Mouse" + (i + 1);
            }
        }

        try {
            for (Field field : GLFW.class.getDeclaredFields()) {
                if (field.getName().startsWith("GLFW_KEY_") && field.getType() == int.class) {
                    if (field.getInt(null) == key) {
                        String fieldName = field.getName().substring("GLFW_KEY_".length());
                        return formatKeyLabel(fieldName);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return "None";
    }

    private String formatKeyLabel(String input) {
        if (input == null || input.isEmpty()) return input;

        if (input.startsWith("LEFT_"))  return "L" + formatWord(input.substring(5));
        if (input.startsWith("RIGHT_")) return "R" + formatWord(input.substring(6));
        if (input.startsWith("KP_"))    return "Numpad" + formatWord(input.substring(3));

        return switch (input) {
            case "PRINT_SCREEN" -> "PrintScreen";
            case "CAPS_LOCK"    -> "CapsLock";
            case "NUM_LOCK"     -> "NumLock";
            case "PAGE_UP"      -> "PageUp";
            case "PAGE_DOWN"    -> "PageDown";
            default -> formatWord(input.replace("_", ""));
        };
    }

    private String formatWord(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        String s = raw.toLowerCase();

        s = s.replace("accent", "");

        s = s.replace("control", "ctrl");
        s = s.replace("super", "Super");
        s = s.replace("minus", "Minus");
        s = s.replace("equals", "Equals");

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

package sweetie.evaware.api.utils.math;

import lombok.experimental.UtilityClass;
import sweetie.evaware.api.system.interfaces.QuickImports;

@UtilityClass
public class MouseUtil implements QuickImports {
    public float getGCD() {
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue() * 0.6f + 0.2f;
        float pow = sensitivity * sensitivity * sensitivity * 8.0f;
        return pow * 0.15f;
    }

    public boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    public boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    public boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height, float round) {
        if (mouseX < x || mouseY < y || mouseX > x + width || mouseY > y + height) {
            return false;
        }

        if (round <= 0) {
            return true;
        }

        float nearestX = Math.max(x + round, Math.min(mouseX, x + width - round));
        float nearestY = Math.max(y + round, Math.min(mouseY, y + height - round));

        float dx = mouseX - nearestX;
        float dy = mouseY - nearestY;
        float distanceSq = dx * dx + dy * dy;

        return distanceSq <= (round * round);
    }

    public boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height, double round) {
        if (mouseX < x || mouseY < y || mouseX > x + width || mouseY > y + height) {
            return false;
        }

        if (round <= 0) {
            return true;
        }

        double nearestX = Math.max(x + round, Math.min(mouseX, x + width - round));
        double nearestY = Math.max(y + round, Math.min(mouseY, y + height - round));

        double dx = mouseX - nearestX;
        double dy = mouseY - nearestY;
        double distanceSq = dx * dx + dy * dy;

        return distanceSq <= (round * round);
    }
}

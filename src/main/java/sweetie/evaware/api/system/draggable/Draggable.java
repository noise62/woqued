package sweetie.evaware.api.system.draggable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.other.WindowResizeEvent;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.math.MathUtil;

@Getter
@Setter
public class Draggable implements QuickImports {
    @Expose
    @SerializedName("x")
    private float x;
    @Expose
    @SerializedName("y")
    private float y;

    public float initialXVal;
    public float initialYVal;

    private float startX, startY;
    private boolean dragging;
    private float width = 0f;
    private float height = 0f;
    @Expose
    @SerializedName("name")
    private final String name;
    private final Module module;

    public Draggable(Module module, String name, float initialXVal, float initialYVal) {
        this.module = module;
        this.name = name;
        this.x = roundToHalf(initialXVal);
        this.y = roundToHalf(initialYVal);
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            if (dragging) clampToScreen();
        }));
    }

    public final void onDraw() {
        if (dragging) {
            x = roundToHalf(MathUtil.interpolate(x, (normaliseX() - startX), .15f));
            y = roundToHalf(MathUtil.interpolate(y, (normaliseY() - startY), .15f));

            clampToScreen();
        }
    }

    public final void onClick(int button) {
        if (button == 0 && isHovering()) {
            boolean anotherDragging = DraggableManager.getInstance().getDraggables().values().stream().anyMatch(Draggable::isDragging);
            if (!anotherDragging) {
                dragging = true;
                startX = (int) (normaliseX() - x);
                startY = (int) (normaliseY() - y);
            }
        }
    }

    public final void onRelease(int button) {
        if (button == 0) dragging = false;
    }

    public boolean isHovering() {
        return normaliseX() > Math.min(x, x + width) && normaliseX() < Math.max(x, x + width) && normaliseY() > Math.min(y, y + height) && normaliseY() < Math.max(y, y + height);
    }

    public int normaliseX() {
        return (int) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    public int normaliseY() {
        return (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }

    private float roundToHalf(float value) {
        return Math.round(value * 2) / 2.0f;
    }

    private void clampToScreen() {
        float margin = 3f;
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();

        if (x < margin) x = margin;
        if (y < margin) y = margin;
        if (x + width > screenWidth - margin) x = screenWidth - width - margin;
        if (y + height > screenHeight - margin) y = screenHeight - height - margin;
    }
}

package worst.woqued.client.ui.clickgui.module;

import lombok.Getter;
import lombok.Setter;
import worst.woqued.api.utils.animation.AnimationUtil;
import worst.woqued.api.utils.animation.Easing;

@Getter
public abstract class ExpandableBase {
    private final AnimationUtil openAnimation = new AnimationUtil();
    @Setter
    private boolean open = false;

    public void updateOpen() {
        openAnimation.update();

        double target = open ? 1.0 : 0.0;
        long duration = open ? 210 : 160;

        Easing easing = open ? Easing.QUINT_OUT : Easing.CUBIC_OUT;

        openAnimation.run(target, duration, easing);
    }

    public void toggleOpen() {
        open = !open;
    }
}
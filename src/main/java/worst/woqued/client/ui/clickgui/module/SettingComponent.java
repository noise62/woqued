package worst.woqued.client.ui.clickgui.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import worst.woqued.api.module.setting.Setting;
import worst.woqued.api.utils.animation.AnimationUtil;
import worst.woqued.client.ui.UIComponent;

@Getter
@RequiredArgsConstructor
public abstract class SettingComponent extends UIComponent {
    private final Setting<?> setting;
    private final AnimationUtil visibleAnimation = new AnimationUtil();

    public void updateHeight(float value) {
        setHeight(scaled(value));
    }
}

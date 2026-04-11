package sweetie.evaware.client.ui.clickgui.module;

import lombok.Getter;
import sweetie.evaware.api.module.setting.Setting;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.client.ui.UIComponent;

@Getter
public abstract class ExpandableComponent extends UIComponent {
    private final ExpandableBase expandableBase = new ExpandableBase() {};

    public void updateOpen() {
        expandableBase.updateOpen();
    }

    public void toggleOpen() {
        expandableBase.toggleOpen();
    }

    public boolean isOpen() {
        return expandableBase.isOpen();
    }

    public float getAnim() {
        return (float) expandableBase.getOpenAnimation().getValue();
    }

    public boolean isNotOver() {
        return getAnim() < 0.8;
    }

    public static abstract class ExpandableSettingComponent extends SettingComponent {
        private final ExpandableBase expandableBase = new ExpandableBase() {};

        public ExpandableSettingComponent(Setting<?> setting) {
            super(setting);
        }

        public void updateOpen() {
            expandableBase.updateOpen();
        }

        public void toggleOpen() {
            expandableBase.toggleOpen();
        }

        public boolean isOpen() {
            return expandableBase.isOpen();
        }

        public void setOpen(boolean value) {
            expandableBase.setOpen(value);
        }

        public float getValue() {
            return (float) expandableBase.getOpenAnimation().getValue();
        }

        public AnimationUtil getAnim() {
            return expandableBase.getOpenAnimation();
        }

        public boolean isNotOver() {
            return getValue() < 0.8;
        }
    }
}
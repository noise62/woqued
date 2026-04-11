package sweetie.evaware.api.module.setting;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
@Setter
public abstract class Setting<T> {
    protected String name;
    protected T value;
    protected Supplier<Boolean> visibilityCondition = () -> true;
    protected Runnable action;

    public Setting(String name) {
        this.name = name;
    }

    public Setting<T> setVisible(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public void runAction() {
        if (this.action != null) {
            action.run();
        }
    }

    protected boolean sameValue(T newValue) {
        if (value == null || newValue == null) return false;

        return value == newValue;
    }

    public Setting<T> onAction(Runnable action) {
        this.action = action;
        return this;
    }

    public boolean isVisible() {
        return visibilityCondition.get();
    }

    public abstract Setting<T> value(T value);
}

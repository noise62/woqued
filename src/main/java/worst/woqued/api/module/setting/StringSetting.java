package worst.woqued.api.module.setting;

import lombok.Getter;

@Getter
public class StringSetting extends Setting<String> {
    public StringSetting(String name) {
        super(name);
    }

    public StringSetting(String name, String defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    @Override
    public Setting<String> value(String value) {
        this.value = value;
        return this;
    }

    public void set(String value) {
        this.value = value;
    }
}
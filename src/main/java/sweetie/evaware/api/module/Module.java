package sweetie.evaware.api.module;

import lombok.Getter;
import lombok.Setter;
import sweetie.evaware.api.system.backend.Configurable;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.client.features.modules.other.ToggleSoundsModule;
import sweetie.evaware.client.features.modules.render.ClickGUIModule;

@Getter
public abstract class Module extends Configurable implements QuickImports {
    private final String name;
    private final Category category;
    @Setter private int bind;

    private boolean enabled;

    public Module() {
        ModuleRegister data = getClass().getAnnotation(ModuleRegister.class);

        if (data == null) try {
            throw new Exception("No data for " + getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.name = data.name();
        this.category = data.category();
        this.bind = data.bind();
    }

    public boolean hasBind() { return bind != -999; }

    public void toggle() {
        setEnabled(!enabled, false);
    }

    public void setEnabled(boolean newState) {
        setEnabled(newState, false);
    }

    public void setEnabled(boolean newState, boolean config) {
        if (enabled == newState) return;

        enabled = newState;
        if (enabled) {
            onEnable();
            onEvent();
        } else {
            onDisable();
            removeAllEvents();
        }

        if (config || this instanceof ClickGUIModule) return;
        ToggleSoundsModule.playToggle(newState);
    }

    public abstract void onEvent();

    public void onEnable() {}
    public void onDisable() {}
}

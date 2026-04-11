package sweetie.evaware.api.system.backend;

import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.module.setting.Setting;
import sweetie.evaware.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Configurable implements QuickImports {
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<EventListener> eventListeners = new ArrayList<>();

    public void addSettings(Setting<?>... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }

    public void addSettings(List<Setting<?>> settings) {
        this.settings.addAll(settings);
    }

    public void addEvents(EventListener... eventListeners) {
        this.eventListeners.addAll(Arrays.asList(eventListeners));
    }

    public void addEvents(List<EventListener> eventListeners) {
        this.eventListeners.addAll(eventListeners);
    }

    public void removeAllEvents() {
        eventListeners.forEach(EventListener::unsubscribe);
    }
    
    public void removeEvents(EventListener... eventListeners) {
        this.eventListeners.removeAll(Arrays.asList(eventListeners));
    }

    public void removeEvents(List<EventListener> eventListeners) {
        this.eventListeners.removeAll(eventListeners);
    }

    public void onEvent() {

    }
}

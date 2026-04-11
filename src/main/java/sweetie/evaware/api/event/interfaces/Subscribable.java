package sweetie.evaware.api.event.interfaces;

import sweetie.evaware.api.event.EventListener;

public interface Subscribable<L, T> {
    EventListener subscribe(L listener);
    void unsubscribe(L listener);
}

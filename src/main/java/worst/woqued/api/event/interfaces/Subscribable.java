package worst.woqued.api.event.interfaces;

import worst.woqued.api.event.EventListener;

public interface Subscribable<L, T> {
    EventListener subscribe(L listener);
    void unsubscribe(L listener);
}

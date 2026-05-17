package worst.woqued.api.event.interfaces;

import worst.woqued.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}
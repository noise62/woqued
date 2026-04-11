package sweetie.evaware.api.event.interfaces;

import sweetie.evaware.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}
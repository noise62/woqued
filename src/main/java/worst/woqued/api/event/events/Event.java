package worst.woqued.api.event.events;

import lombok.Getter;
import lombok.Setter;
import worst.woqued.api.event.Flora;

@Getter
@Setter
public class Event<T> extends Flora<T> {
    private boolean cancel = false;

    @SuppressWarnings("unchecked")
    protected T getSelf() {
        return (T) this;
    }

    public boolean call() {
        cancel = false;
        notify(getSelf());
        return cancel;
    }

    public boolean call(T any) {
        cancel = false;
        notify(any);
        return cancel;
    }
}

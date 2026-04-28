package worst.woqued.api.event.interfaces;

public interface Notifiable<E> {
    void notify(E event);
}

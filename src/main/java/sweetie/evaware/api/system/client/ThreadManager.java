package sweetie.evaware.api.system.client;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThreadManager {
    public Thread run(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }
}
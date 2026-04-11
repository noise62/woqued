package sweetie.evaware.api.utils.task;

import sweetie.evaware.api.module.Module;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TaskProcessor<T> {
    private int tickCounter = 0;
    private final PriorityQueue<Task<T>> activeTasks = new PriorityQueue<>(
            Comparator.<Task<T>>comparingInt(task -> task.priority).reversed()
    );

    public void tick(int deltaTime) {
        tickCounter += deltaTime;

        while (!activeTasks.isEmpty() && activeTasks.peek().expiresIn <= tickCounter) {
            Task<T> task = activeTasks.poll();
            task.run();
        }
    }

    public void addTask(Task<T> task) {
        activeTasks.removeIf(t -> t.provider == task.provider);
        task.expiresIn += tickCounter;
        activeTasks.add(task);
    }

    public T fetchActiveTaskValue() {
        while (!activeTasks.isEmpty() && (activeTasks.peek().expiresIn <= tickCounter || !activeTasks.peek().provider.isEnabled())) {
            activeTasks.poll();
        }

        if (activeTasks.isEmpty()) {
            return null;
        }

        return activeTasks.peek().value;
    }

    public static class Task<T> {
        public int expiresIn;
        public final int priority;
        public final Module provider;
        public final T value;

        public Task(int expiresIn, int priority, Module provider, T value) {
            this.expiresIn = expiresIn;
            this.priority = priority;
            this.provider = provider;
            this.value = value;
        }

        public void run() {
        }

        @Override
        public String toString() {
            return "Task(expiresIn=" + expiresIn + ", priority=" + priority + ", provider=" + provider + ", value=" + value + ")";
        }
    }
}

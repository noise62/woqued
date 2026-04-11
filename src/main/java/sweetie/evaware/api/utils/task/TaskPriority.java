package sweetie.evaware.api.utils.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskPriority {
    CRITICAL(5),
    REQUIRED(4),

    HIGH(3),
    MEDIUM_HIGH(2),
    MEDIUM(1),

    NORMAL(0),
    LOW(-1),
    LOWEST(-2);

    private final int priority;
}
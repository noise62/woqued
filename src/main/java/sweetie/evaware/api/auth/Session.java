package sweetie.evaware.api.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Session {
    private final String username;
    private final UUID uuid;
    private final String token;
    private final String type;
}

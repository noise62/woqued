package sweetie.evaware.api.auth;

import java.util.UUID;

public class UUIDUtils {
    public static UUID parseUuid(String string) {
        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException e) {
            return uuidFromUnformatted(string);
        }
    }

    private static UUID uuidFromUnformatted(String input) {
        if (input.length() != 32) {
            throw new IllegalArgumentException("ХУЙ");
        }

        long mostSigBits = Long.parseUnsignedLong(input.substring(0, 16), 16);
        long leastSigBits = Long.parseUnsignedLong(input.substring(16, 32), 16);
        return new UUID(mostSigBits, leastSigBits);
    }

    public static UUID generateOfflinePlayerUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
    }
}

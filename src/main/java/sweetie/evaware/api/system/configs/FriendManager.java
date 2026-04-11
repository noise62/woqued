package sweetie.evaware.api.system.configs;

import lombok.Getter;
import sweetie.evaware.api.system.files.AbstractFile;

public class FriendManager extends AbstractFile {
    @Getter private static final FriendManager instance = new FriendManager();

    @Override
    public String fileName() {
        return "friends";
    }
}

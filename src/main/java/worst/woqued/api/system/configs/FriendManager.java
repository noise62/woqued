package worst.woqued.api.system.configs;

import lombok.Getter;
import worst.woqued.api.system.files.AbstractFile;

public class FriendManager extends AbstractFile {
    @Getter private static final FriendManager instance = new FriendManager();

    @Override
    public String fileName() {
        return "friends";
    }
}

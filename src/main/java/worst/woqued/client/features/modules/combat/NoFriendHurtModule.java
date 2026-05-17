package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

@ModuleRegister(name = "No Friend Hurt", category = Category.COMBAT)
public class NoFriendHurtModule extends Module {
    @Getter private static final NoFriendHurtModule instance = new NoFriendHurtModule();

    @Override
    public void onEvent() {

    }
}

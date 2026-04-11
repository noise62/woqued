package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

@ModuleRegister(name = "No Friend Hurt", category = Category.COMBAT)
public class NoFriendHurtModule extends Module {
    @Getter private static final NoFriendHurtModule instance = new NoFriendHurtModule();

    @Override
    public void onEvent() {

    }
}

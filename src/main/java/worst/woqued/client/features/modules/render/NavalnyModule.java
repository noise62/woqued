package worst.woqued.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.utils.other.SoundUtil;

@Getter
@ModuleRegister(name = "Navalny", category = Category.RENDER)
public class NavalnyModule extends Module {
    @Getter private static final NavalnyModule instance = new NavalnyModule();

    private static final Identifier NAVALNY_SKIN = Identifier.of("evaware", "char.png");

    @Override
    public void onEnable() {
        SoundUtil.playSound(SoundUtil.NAVALNY_EVENT);
    }

    @Override
    public void onDisable() {
        SoundUtil.stopSounds();
    }

    @Override
    public void onEvent() {
    }

    public Identifier getNavalnySkin() {
        return NAVALNY_SKIN;
    }
}
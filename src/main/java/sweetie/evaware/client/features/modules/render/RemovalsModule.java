package sweetie.evaware.client.features.modules.render;

import lombok.Getter;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;

import java.util.Arrays;

@ModuleRegister(name = "Removals", category = Category.RENDER)
public class RemovalsModule extends Module {
    @Getter private static final RemovalsModule instance = new RemovalsModule();

    private final String[] elements = {
            "Fire overlay", "Hurt camera", "Inwall overlay", "Water overlay",
            "Scoreboard", "Glow effect", "Bad effects", "Boss bar"
    };

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Remove").value(
            Arrays.stream(elements)
                    .map(name -> new BooleanSetting(name).value(false))
                    .toArray(BooleanSetting[]::new)
    );

    public RemovalsModule() {
        addSettings(remove);
    }

    public boolean isFireOverlay()   { return isEnabled() && remove.isEnabled("Fire overlay"); }
    public boolean isHurtCamera()    { return isEnabled() && remove.isEnabled("Hurt camera"); }
    public boolean isInwallOverlay() { return isEnabled() && remove.isEnabled("Inwall overlay"); }
    public boolean isWaterOverlay()  { return isEnabled() && remove.isEnabled("Water overlay"); }
    public boolean isScoreboard()    { return isEnabled() && remove.isEnabled("Scoreboard"); }
    public boolean isGlowEffect()    { return isEnabled() && remove.isEnabled("Glow effect"); }
    public boolean isBadEffects()    { return isEnabled() && remove.isEnabled("Bad effects"); }
    public boolean isBossBar()       { return isEnabled() && remove.isEnabled("Boss bar"); }

    @Override
    public void onEvent() {

    }
}
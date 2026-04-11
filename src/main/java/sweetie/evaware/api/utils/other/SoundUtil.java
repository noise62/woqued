package sweetie.evaware.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.client.features.modules.other.ToggleSoundsModule;

@UtilityClass
public class SoundUtil implements QuickImports {
    // smooth toggle sound
    private final Identifier ENABLE_SMOOTH_SOUND = Identifier.of(path() + "smooth_on");
    public SoundEvent ENABLE_SMOOTH_EVENT = SoundEvent.of(ENABLE_SMOOTH_SOUND);
    private final Identifier DISABLE_SMOOTH_SOUND = Identifier.of(path() + "smooth_off");
    public SoundEvent DISABLE_SMOOTH_EVENT = SoundEvent.of(DISABLE_SMOOTH_SOUND);

    // cel toggle sound
    private final Identifier ENABLE_CEL_SOUND = Identifier.of(path() + "celestial_on");
    public SoundEvent ENABLE_CEL_EVENT = SoundEvent.of(ENABLE_CEL_SOUND);
    private final Identifier DISABLE_CEL_SOUND = Identifier.of(path() + "celestial_off");
    public SoundEvent DISABLE_CEL_EVENT = SoundEvent.of(DISABLE_CEL_SOUND);

    // nur toggle sound
    private final Identifier ENABLE_NU_SOUND = Identifier.of(path() + "nursultan_on");
    public SoundEvent ENABLE_NU_EVENT = SoundEvent.of(ENABLE_NU_SOUND);
    private final Identifier DISABLE_NU_SOUND = Identifier.of(path() + "nursultan_off");
    public SoundEvent DISABLE_NU_EVENT = SoundEvent.of(DISABLE_NU_SOUND);

    // akrien toggle sound
    private final Identifier ENABLE_AK_SOUND = Identifier.of(path() + "akrien_on");
    public SoundEvent ENABLE_AK_EVENT = SoundEvent.of(ENABLE_AK_SOUND);
    private final Identifier DISABLE_AK_SOUND = Identifier.of(path() + "akrien_off");
    public SoundEvent DISABLE_AK_EVENT = SoundEvent.of(DISABLE_AK_SOUND);

    // tech toggle sound
    private final Identifier ENABLE_TECH_SOUND = Identifier.of(path() + "tech_on");
    public SoundEvent ENABLE_TECH_EVENT = SoundEvent.of(ENABLE_TECH_SOUND);
    private final Identifier DISABLE_TECH_SOUND = Identifier.of(path() + "tech_off");
    public SoundEvent DISABLE_TECH_EVENT = SoundEvent.of(DISABLE_TECH_SOUND);

    // blop toggle sound
    private final Identifier ENABLE_BLOP_SOUND = Identifier.of(path() + "blop_on");
    public SoundEvent ENABLE_BLOP_EVENT = SoundEvent.of(ENABLE_BLOP_SOUND);
    private final Identifier DISABLE_BLOP_SOUND = Identifier.of(path() + "blop_off");
    public SoundEvent DISABLE_BLOP_EVENT = SoundEvent.of(DISABLE_BLOP_SOUND);

    public void load() {
        Registry.register(Registries.SOUND_EVENT, ENABLE_SMOOTH_SOUND, ENABLE_SMOOTH_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_SMOOTH_SOUND, DISABLE_SMOOTH_EVENT);

        Registry.register(Registries.SOUND_EVENT, ENABLE_CEL_SOUND, ENABLE_CEL_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_CEL_SOUND, DISABLE_CEL_EVENT);

        Registry.register(Registries.SOUND_EVENT, ENABLE_NU_SOUND, ENABLE_NU_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_NU_SOUND, DISABLE_NU_EVENT);

        Registry.register(Registries.SOUND_EVENT, ENABLE_AK_SOUND, ENABLE_AK_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_AK_SOUND, DISABLE_AK_EVENT);

        Registry.register(Registries.SOUND_EVENT, ENABLE_TECH_SOUND, ENABLE_TECH_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_TECH_SOUND, DISABLE_TECH_EVENT);

        Registry.register(Registries.SOUND_EVENT, ENABLE_BLOP_SOUND, ENABLE_BLOP_EVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_BLOP_SOUND, DISABLE_BLOP_EVENT);
    }

    public void playSound(SoundEvent sound) {
        if (mc.player != null && mc.world != null && mc.getCameraEntity() != null)
            mc.world.playSound(mc.player, mc.getCameraEntity().getBlockPos(), sound, SoundCategory.BLOCKS, ToggleSoundsModule.getInstance().volume.getValue() / 100f, 1f);
    }

    private String path() {
        return "evaware:";
    }
}

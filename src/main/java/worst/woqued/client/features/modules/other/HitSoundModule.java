package worst.woqued.client.features.modules.other;

import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.world.AttackEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.other.SoundUtil;

import net.minecraft.sound.SoundEvent;
import java.util.Optional;

@ModuleRegister(name = "Hit Sound", category = Category.OTHER)
public class HitSoundModule extends Module {
    @Getter private static final HitSoundModule instance = new HitSoundModule();

    private final ModeSetting sound = new ModeSetting("Sound").value("Otkazano").values("Otkazano", "Awp", "Photo", "Am", "Rust", "Cs");
    private final SliderSetting volume = new SliderSetting("Volume").value(60f).range(1f, 100f).step(1f);

    public HitSoundModule() {
        addSettings(sound, volume);
    }

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            Optional<SoundEvent> soundEvent = switch (sound.getValue()) {
                case "Otkazano" -> Optional.of(SoundUtil.OTKAZANO_EVENT);
                case "Awp" -> Optional.of(SoundUtil.AWP_EVENT);
                case "Photo" -> Optional.of(SoundUtil.PHOTO_EVENT);
                case "Am" -> Optional.of(SoundUtil.AM_EVENT);
                case "Rust" -> Optional.of(SoundUtil.RUST_EVENT);
                case "Cs" -> Optional.of(SoundUtil.CS_EVENT);
                default -> Optional.empty();
            };
            soundEvent.ifPresent(se -> SoundUtil.playSound(se, volume.getValue() / 100f));
        }));

        addEvents(attackEvent);
    }
}
package worst.woqued.client.features.modules.movement.speed;


import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.move.TravelEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.system.backend.Choice;
import worst.woqued.client.features.modules.movement.speed.modes.SpeedGrim;
import worst.woqued.client.features.modules.movement.speed.modes.SpeedVanilla;
import worst.woqued.client.features.modules.movement.speed.modes.SpeedMetaHvH;

@ModuleRegister(name = "Speed", category = Category.MOVEMENT)
public class SpeedModule extends Module {
    @Getter private static final SpeedModule instance = new SpeedModule();

    private final SpeedGrim speedGrim = new SpeedGrim(() -> getMode().is("Grim"));
    private final SpeedVanilla speedVanilla = new SpeedVanilla(() -> getMode().is("Vanilla"));
    private final SpeedMetaHvH speedMetaHvH = new SpeedMetaHvH(() -> getMode().is("MetaHvH"));

    private final SpeedMode[] modes = new SpeedMode[]{
            speedVanilla, speedGrim, speedMetaHvH
    };

    private SpeedMode currentMode = speedGrim;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(speedGrim.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                currentMode = (SpeedMode) Choice.getChoiceByName(getMode().getValue(), modes);
            });

    public SpeedModule() {
        addSettings(mode);

        addSettings(speedGrim.getSettings());
        addSettings(speedVanilla.getSettings());
    }

    @Override
    public void toggle() {
        super.toggle();
        currentMode.toggle();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentMode.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentMode.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        EventListener travelEvent = TravelEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onTravel();
        }));

        addEvents(updateEvent, travelEvent);
    }
}
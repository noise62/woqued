package sweetie.evaware.client.features.modules.movement.speed;


import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.move.TravelEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.client.features.modules.movement.speed.modes.SpeedGrim;
import sweetie.evaware.client.features.modules.movement.speed.modes.SpeedVanilla;

@ModuleRegister(name = "Speed", category = Category.MOVEMENT)
public class SpeedModule extends Module {
    @Getter private static final SpeedModule instance = new SpeedModule();

    private final SpeedGrim speedGrim = new SpeedGrim(() -> getMode().is("Grim"));
    private final SpeedVanilla speedVanilla = new SpeedVanilla(() -> getMode().is("Vanilla"));

    private final SpeedMode[] modes = new SpeedMode[]{
            speedVanilla, speedGrim
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
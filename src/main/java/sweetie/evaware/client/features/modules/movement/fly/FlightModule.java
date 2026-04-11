package sweetie.evaware.client.features.modules.movement.fly;


import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.move.MotionEvent;
import sweetie.evaware.api.event.events.player.move.TravelEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.client.features.modules.movement.fly.modes.FlightGrim;
import sweetie.evaware.client.features.modules.movement.fly.modes.FlightVanilla;

@ModuleRegister(name = "Flight", category = Category.MOVEMENT)
public class FlightModule extends Module {
    @Getter private static final FlightModule instance = new FlightModule();

    private final FlightGrim flightGrim = new FlightGrim(() -> getMode().is("Grim"), this);
    private final FlightVanilla flightVanilla = new FlightVanilla(() -> getMode().is("Vanilla"));

    private final FlightMode[] modes = new FlightMode[]{
            flightVanilla, flightGrim
    };

    private FlightMode currentMode = flightGrim;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(flightGrim.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                currentMode = (FlightMode) Choice.getChoiceByName(getMode().getValue(), modes);
            });

    public FlightModule() {
        addSettings(mode);

        addSettings(flightGrim.getSettings());
        addSettings(flightVanilla.getSettings());
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

        EventListener motionEvent = MotionEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onMotion(event);
        }));

        addEvents(updateEvent, motionEvent);
    }
}
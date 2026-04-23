package worst.woqued.client.features.modules.movement.fly;


import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.move.MotionEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.system.backend.Choice;
import worst.woqued.client.features.modules.movement.fly.modes.FlightGrim;
import worst.woqued.client.features.modules.movement.fly.modes.FlightVanilla;

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
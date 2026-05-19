package worst.woqued.client.features.modules.movement.noslow;

import lombok.Getter;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.system.backend.Choice;
import worst.woqued.client.features.modules.movement.noslow.modes.*;
import worst.woqued.client.features.modules.movement.noslow.modes.NoSlowCancel;
import worst.woqued.client.features.modules.movement.noslow.modes.NoSlowFunTime;
import worst.woqued.client.features.modules.movement.noslow.modes.NoSlowGrim;
import worst.woqued.client.features.modules.movement.noslow.modes.NoSlowReallyWorld;
import worst.woqued.client.features.modules.movement.noslow.modes.NoSlowSlotUpdate;

@ModuleRegister(name = "No Slow", category = Category.MOVEMENT)
public class NoSlowModule extends Module {
    @Getter private static final NoSlowModule instance = new NoSlowModule();

    private final NoSlowCancel noSlowCancel = new NoSlowCancel();
    private final NoSlowSlotUpdate noSlowSlotUpdate = new NoSlowSlotUpdate();
    private final NoSlowGrim noSlowGrim = new NoSlowGrim();
    private final NoSlowReallyWorld noSlowReallyWorld = new NoSlowReallyWorld();
    private final NoSlowFunTime noSlowFunTime = new NoSlowFunTime();

    private final NoSlowMode[] modes = new NoSlowMode[]{
            noSlowCancel, noSlowSlotUpdate, noSlowGrim, noSlowReallyWorld, noSlowFunTime
    };

    private NoSlowMode currentMode = noSlowCancel;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Cancel").values(
            Choice.getValues(modes)
    ).onAction(() -> {
        currentMode = (NoSlowMode) Choice.getChoiceByName(getMode().getValue(), modes);
    });
    @Getter private final ModeSetting grimMode = new ModeSetting("Grim mode").value("Tick").values("Tick", "Old").setVisible(() -> mode.is("Grim")).onAction(() -> {
        noSlowGrim.bypassType = switch (getGrimMode().getValue()) {
            case "Tick" -> NoSlowGrim.BypassType.TICK;
            default -> NoSlowGrim.BypassType.OLD;
        };
    });

    public NoSlowModule() {
        addSettings(mode, grimMode);
    }

    public boolean doUseNoSlow() {
        return isEnabled() && mc.player.isUsingItem() && currentMode.slowingCancel();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onTick();
        }));

        addEvents(updateEvent, tickEvent);
    }
}

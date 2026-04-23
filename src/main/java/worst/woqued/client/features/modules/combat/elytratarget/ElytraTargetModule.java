package worst.woqued.client.features.modules.combat.elytratarget;

import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.other.RotationUpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

@ModuleRegister(name = "Elytra Target", category = Category.COMBAT)
public class ElytraTargetModule extends Module {
    @Getter private static final ElytraTargetModule instance = new ElytraTargetModule();
    public final ElytraRotationProcessor elytraRotationProcessor = new ElytraRotationProcessor(this);

    public ElytraTargetModule() {
        addSettings(elytraRotationProcessor.getSettings());
    }

    @Override
    public void onEvent() {
        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            elytraRotationProcessor.processRotation();
        }));

        addEvents(rotationUpdateEvent);
    }
}

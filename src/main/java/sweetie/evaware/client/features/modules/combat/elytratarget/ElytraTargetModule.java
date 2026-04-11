package sweetie.evaware.client.features.modules.combat.elytratarget;

import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.other.RotationUpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

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

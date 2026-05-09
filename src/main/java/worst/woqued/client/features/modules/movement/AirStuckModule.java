package worst.woqued.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.util.math.Vec3d;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;

@ModuleRegister(name = "Air Stuck", category = Category.MOVEMENT)
public class AirStuckModule extends Module {

    @Getter
    private static final AirStuckModule instance = new AirStuckModule();

    private final BooleanSetting cancelMovement =
            new BooleanSetting("Cancel movement").value(true);

    private Vec3d freezePosition = Vec3d.ZERO;

    public AirStuckModule() {
        addSettings(cancelMovement);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            freezePosition = mc.player.getPos();
        }
    }

    @Override
    public void onDisable() {
        freezePosition = Vec3d.ZERO;
    }

    @Override
    public void onEvent() {

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || freezePosition == Vec3d.ZERO) return;

            mc.player.setPosition(freezePosition);
            mc.player.setVelocity(Vec3d.ZERO);

            if (cancelMovement.getValue()) {
                mc.player.input.movementForward = 0;
                mc.player.input.movementSideways = 0;
            }
        }));

        addEvents(updateEvent);
    }
}
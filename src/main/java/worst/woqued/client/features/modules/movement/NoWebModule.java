package worst.woqued.client.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.utils.player.PlayerUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleRegister(name = "No Web", category = Category.MOVEMENT)
public class NoWebModule extends Module {
    @Getter private static final NoWebModule instance = new NoWebModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (PlayerUtil.isInWeb()) {
                double motionY = mc.options.jumpKey.isPressed() ? 1.3 : mc.options.sneakKey.isPressed() ? -1.3 : 0;
                float yaw = mc.player.getYaw() * ((float)Math.PI / 180.0F);
                float f = mc.player.forwardSpeed * 0.633F;
                float s = mc.player.sidewaysSpeed * 0.633F;
                
                if (f != 0 || s != 0) {
                    mc.player.setVelocity(-MathHelper.sin(yaw) * f + MathHelper.cos(yaw) * s, motionY,
                                          MathHelper.cos(yaw) * f + MathHelper.sin(yaw) * s);
                } else {
                    mc.player.setVelocity(0, motionY, 0);
                }
            }
        }));

        addEvents(updateEvent);
    }
}

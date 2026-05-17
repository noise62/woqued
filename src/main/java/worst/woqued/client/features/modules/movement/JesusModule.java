package worst.woqued.client.features.modules.movement;

import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.utils.player.MoveUtil;

@ModuleRegister(name = "Jesus", category = Category.MOVEMENT)
public class JesusModule extends Module {
    @Getter private static final JesusModule instance = new JesusModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));

        addEvents(updateEvent);
    }

    private void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            var speedEffect = mc.player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.SPEED);
            var slowEffect = mc.player.getStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);

            float appliedSpeed = 0.47f;

            if (speedEffect != null) {
                if (speedEffect.getAmplifier() == 2) {
                    appliedSpeed = 0.47f * 1.2f;
                } else if (speedEffect.getAmplifier() == 1) {
                    appliedSpeed = 0.47f * 1.05f;
                }
            } else {
                appliedSpeed = 0.47f * 0.7f;
            }

            if (slowEffect != null) {
                appliedSpeed *= 0.8f;
            }

            MoveUtil.setSpeed(appliedSpeed);

            boolean isMoving = MoveUtil.w() || MoveUtil.s() || MoveUtil.a() || MoveUtil.d();

            if (!isMoving) {
                mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            }

            double yMotion = mc.options.jumpKey.isPressed() ? 0.025 : 0.005;
            mc.player.setVelocity(mc.player.getVelocity().x, yMotion, mc.player.getVelocity().z);
        }
    }
}
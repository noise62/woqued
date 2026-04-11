package sweetie.evaware.client.features.modules.movement.speed.modes;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.player.MoveUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.api.system.client.TimerManager;
import sweetie.evaware.api.utils.task.TaskPriority;
import sweetie.evaware.client.features.modules.movement.speed.SpeedMode;
import sweetie.evaware.client.features.modules.movement.speed.SpeedModule;

import java.util.function.Supplier;

public class SpeedGrim extends SpeedMode {
    @Override
    public String getName() {
        return "Grim";
    }

    public BypassType bypassType;
    private boolean boosting;
    private final TimerUtil timerUtil = new TimerUtil();

    @Getter private final ModeSetting grimType = new ModeSetting("Grim mode").value(BypassType.COLLIDE)
            .values(BypassType.values())
            .onAction(() -> {
                bypassType = switch (getGrimType().getValue()) {
                    case "Timer" -> SpeedGrim.BypassType.TIMER;
                    case "Collide new" -> SpeedGrim.BypassType.COLLIDE_NEW;
                    default -> SpeedGrim.BypassType.COLLIDE;
                };
            });

    public SpeedGrim(Supplier<Boolean> condition) {
        grimType.setVisible(condition);
        addSettings(grimType);
    }

    @Override
    public void onTravel() {
        switch (bypassType) {
            case COLLIDE, COLLIDE_NEW -> {
                boolean newMode = bypassType == BypassType.COLLIDE_NEW;
                int collisions = 0;
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof LivingEntity living) {
                        if (living == mc.player) continue;
                        if (living instanceof ArmorStandEntity) continue;
                        if (PlayerUtil.hasCollisionWith(living, newMode ? 0f : 1f)) {
                            collisions++;
                        }
                    }
                }

                if (collisions > 0) {
                    double[] forward = MoveUtil.forward(0.08 * collisions);
                    mc.player.addVelocity(forward[0], 0.0, forward[1]);
                }
            }


            default -> {
                if (timerUtil.finished(1100)) {
                    boosting = true;
                }

                if (timerUtil.finished(7000)) {
                    boosting = false;
                    timerUtil.reset();
                }

                TimerManager.getInstance().addTimer(boosting ? mc.player.age % 2 == 0 ? 1.5f : 1.2f : 0.05f, TaskPriority.HIGH, SpeedModule.getInstance(), 1);
            }
        }
    }

    public enum BypassType implements ModeSetting.NamedChoice {
        COLLIDE("Collide"),
        COLLIDE_NEW("Collide new"),
        TIMER("Timer");

        private final String name;

        BypassType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}

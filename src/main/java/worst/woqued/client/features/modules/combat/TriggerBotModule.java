package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.MultiBooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.combat.ClickScheduler;
import worst.woqued.api.utils.combat.TargetManager;

@ModuleRegister(name = "Trigger Bot", category = Category.COMBAT)
public class TriggerBotModule extends Module {
    @Getter private static final TriggerBotModule instance = new TriggerBotModule();

    private final SliderSetting distance = new SliderSetting("Distance").value(3f).range(2.5f, 6f).step(0.1f);

    @Getter private final ModeSetting clickMode = new ModeSetting("Click mode").value("1.9").values("1.9", "1.8");
    @Getter private final SliderSetting cps = new SliderSetting("CPS").value(9f).range(1f, 20f).step(0.5f).setVisible(() -> clickMode.is("1.8"));

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(true),
            new BooleanSetting("Animals").value(true)
    );

    private final BooleanSetting onlyCrits = new BooleanSetting("Only crits").value(true);
    private final BooleanSetting smartCrits = new BooleanSetting("Smart crits").value(true).setVisible(onlyCrits::getValue);

    private TargetManager.EntityFilter entityFilter;
    private final ClickScheduler clickScheduler = new ClickScheduler();
    private long lastClickTime = 0;

    public TriggerBotModule() {
        entityFilter = new TargetManager.EntityFilter(targets.getList());
        addSettings(distance, clickMode, cps, targets, onlyCrits, smartCrits);
    }

    @Override
    public void onEnable() {
        lastClickTime = 0;
        if (clickMode.is("1.8")) {
            long delay = (long) (1000.0 / cps.getValue());
            clickScheduler.recalculate(delay);
        }
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            if (clickMode.is("1.8")) {
                if (!clickScheduler.isCooldownComplete()) return;
            } else {
                float cooldown = mc.player.getAttackCooldownProgress(0.5f);
                if (cooldown < 0.9f) return;
                long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
                if (timeSinceLastClick < 500) return;
            }

            Entity target = getCrosshairTarget();
            if (target == null) return;

            if (!isValidTarget(target)) return;

            attack(target);
            lastClickTime = System.currentTimeMillis();

            if (clickMode.is("1.8")) {
                long delay = (long) (1000.0 / cps.getValue());
                clickScheduler.recalculate(delay);
            }
        }));

        addEvents(updateEvent);
    }

    private Entity getCrosshairTarget() {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return null;

        EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
        Entity entity = entityHit.getEntity();

        if (entity == null) return null;
        if (entity.distanceTo(mc.player) > distance.getValue()) return null;
        if (!mc.player.canSee(entity)) return null;

        return entity;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return false;
        if (!livingEntity.isAlive()) return false;
        if (entity == mc.player) return false;

        entityFilter.targetSettings = targets.getList();
        return entityFilter.isValid(livingEntity);
    }

    private void attack(Entity target) {
        if (clickMode.is("1.9") && onlyCrits.getValue()) {
            if (mc.player.isOnGround()) {
                if (smartCrits.getValue()) {
                    if (!mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
                        return;
                    }
                }
                mc.player.jump();
            }
        }
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
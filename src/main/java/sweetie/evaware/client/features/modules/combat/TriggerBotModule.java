package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.combat.ClickScheduler;
import sweetie.evaware.api.utils.combat.TargetManager;

@ModuleRegister(name = "Trigger Bot", category = Category.COMBAT)
public class TriggerBotModule extends Module {
    @Getter private static final TriggerBotModule instance = new TriggerBotModule();

    private final SliderSetting range = new SliderSetting("Range").value(6.0f).range(1.0f, 10.0f).step(0.5f);
    private final ModeSetting aimPoint = new ModeSetting("Aim Point").value("Body").values("Head", "Body", "Legs");

    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls").value(false);
    private final BooleanSetting onlyCrits = new BooleanSetting("Only Crits").value(false);

    private TargetManager.EntityFilter entityFilter;
    private final ClickScheduler clickScheduler = new ClickScheduler();

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(true),
            new BooleanSetting("Animals").value(false)
    );

    public TriggerBotModule() {
        entityFilter = new TargetManager.EntityFilter(targets.getList());
        addSettings(range, aimPoint, throughWalls, onlyCrits, targets);
    }

    @Override
    public void onEnable() {
        clickScheduler.recalculate(500);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            if (!clickScheduler.isCooldownComplete()) return;

            Entity target = getCrosshairTarget();
            if (target == null) return;

            if (!isValidTarget(target)) return;

            if (onlyCrits.getValue() && mc.player.fallDistance <= 0.0f) return;

            attack(target);
            clickScheduler.recalculate(500);
        }));

        addEvents(updateEvent);
    }

    private Entity getCrosshairTarget() {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return null;

        EntityHitResult entityHit = (EntityHitResult) mc.crosshairTarget;
        Entity entity = entityHit.getEntity();

        if (entity == null) return null;
        if (entity.distanceTo(mc.player) > range.getValue()) return null;
        if (!throughWalls.getValue() && !mc.player.canSee(entity)) return null;

        return entity;
    }

    private Vec3d getAimPoint(Entity entity) {
        var box = entity.getBoundingBox();
        double minY = box.minY;
        double maxY = box.maxY;
        double height = maxY - minY;
        double centerX = box.minX + (box.maxX - box.minX) * 0.5;
        double centerZ = box.minZ + (box.maxZ - box.minZ) * 0.5;

        switch (aimPoint.getValue()) {
            case "Head":
                return new Vec3d(centerX, maxY - height * 0.1, centerZ);
            case "Body":
                return new Vec3d(centerX, minY + height * 0.5, centerZ);
            case "Legs":
                return new Vec3d(centerX, minY + height * 0.15, centerZ);
            default:
                return box.getCenter();
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) return false;
        if (!livingEntity.isAlive()) return false;
        if (entity == mc.player) return false;

        entityFilter.targetSettings = targets.getList();
        return entityFilter.isValid(livingEntity);
    }

    private void attack(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}

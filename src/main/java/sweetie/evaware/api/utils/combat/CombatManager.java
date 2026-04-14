package sweetie.evaware.api.utils.combat;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.move.SprintEvent;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.api.utils.rotation.RaytracingUtil;
import sweetie.evaware.api.utils.rotation.rotations.FunTimeRotation;
import sweetie.evaware.client.features.modules.movement.SprintModule;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatManager implements QuickImports {
    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final SprintManager sprintManager = new SprintManager(SprintManager.SprintType.LEGIT);
    private final ShieldBreakManager shieldBreakManager = new ShieldBreakManager();

    public CombatManager() {
        SprintEvent.getInstance().subscribe(new Listener<>(1, event -> {
            boolean oneTick = clickScheduler.isOneTickBeforeAttack();

            boolean rule = configurable != null && configurable.target != null
                    && configurable.onlyCrits
                    && !mc.player.isOnGround() && !shouldCancelCrit()
                    && (oneTick && mc.player.getVelocity().y <= .16477328182606651);

            sprintManager.legitSprint(event, rule);
        }));
    }

    @Getter @Setter
    private CombatExecutor.CombatConfigurable configurable;

    public void handleAttack() {
        sprintManager.sprintType = switch (SprintModule.getInstance().mode.getValue()) {
            case "Packet" -> SprintManager.SprintType.PACKET;
            case "Legit" -> SprintManager.SprintType.LEGIT;
            default -> SprintManager.SprintType.NONE;
        };

        FunTimeRotation.updateAttackState(canAttack());

        if (canAttack()) {
            if (isRaytraceFailed(getRaytraceEntity())) return;

            if (mc.player.isBlocking() && configurable.alwaysShield) {
                mc.interactionManager.stopUsingItem(mc.player);
            }

            sprintManager.packetSprint(false);
            mc.interactionManager.attackEntity(mc.player, configurable.target);
            mc.player.swingHand(Hand.MAIN_HAND);
            sprintManager.packetSprint(true);
            clickScheduler.recalculate(500);
        }
    }

    public boolean canAttack() {
        if (isCooldownNotComplete()) return false;
        if (configurable.noAttackIfEat && PlayerUtil.isEating()) return false;

        if (configurable.shieldBreak && configurable.target instanceof PlayerEntity player && shieldBreakManager.shouldBreakShield(player)) {
            return true;
        }

        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && configurable.onlyCrits && configurable.smartCrits)
            return true;

        if (!mc.options.jumpKey.isPressed() && PlayerUtil.isAboveWater())
            return true;

        return shouldDoHit() || !configurable.onlyCrits;
    }

    private boolean isRaytraceFailed(Entity targetEntity) {
        return configurable.raytrace && targetEntity != configurable.target && !mc.player.isGliding();
    }

    private boolean isCooldownNotComplete() {
        return !clickScheduler.isCooldownComplete();
    }

    private boolean shouldCancelCrit() {
        return mc.player.hasStatusEffect(StatusEffects.BLINDNESS) ||
                mc.player.hasStatusEffect(StatusEffects.LEVITATION) ||
                mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) ||
                PlayerUtil.isInWeb() ||
                mc.player.isInLava() ||
                mc.player.isClimbing() ||
                mc.player.isRiding() ||
                mc.player.hasVehicle() ||
                mc.player.isSubmergedInWater() ||
                mc.player.hasNoGravity() ||
                mc.player.getAbilities().flying;
    }

    private boolean shouldDoHit() {
        return (!mc.player.isOnGround() && mc.player.fallDistance > 0.0f) || shouldCancelCrit();
    }

    private Entity getRaytraceEntity() {
        EntityHitResult entityHitResult = RaytracingUtil.raytraceEntity(
                configurable.distance,
                configurable.rotation,
                configurable.ignoreWalls
        );

        if (entityHitResult != null) {
            return entityHitResult.getEntity();
        }

        return null;
    }
}

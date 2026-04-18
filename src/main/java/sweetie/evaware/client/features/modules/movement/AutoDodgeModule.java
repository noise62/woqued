package sweetie.evaware.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.MovementInputEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.system.client.TimerManager;
import sweetie.evaware.api.utils.player.DirectionalInput;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationStrategy;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;
import sweetie.evaware.api.utils.task.TaskPriority;
import sweetie.evaware.client.features.modules.combat.AuraModule;
import sweetie.evaware.api.utils.rotation.RotationUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ModuleRegister(name = "Auto Dodge", category = Category.MOVEMENT)
public class AutoDodgeModule extends Module {
    @Getter private static final AutoDodgeModule instance = new AutoDodgeModule();

    private static final double SAFE_DISTANCE = 1.5 * 0.3 + 1.5 * 0.5; // ~0.9
    private static final double SAFE_DISTANCE_WITH_PADDING = 0.3 * 5; // 1.5

    private final BooleanSetting allowRotationChange = new BooleanSetting("AllowRotationChange").value(false);
    private final BooleanSetting allowJump = new BooleanSetting("AllowJump").value(true).setVisible(allowRotationChange::getValue);
    private final BooleanSetting allowTimer = new BooleanSetting("AllowTimer").value(false);
    private final SliderSetting timerSpeed = new SliderSetting("TimerSpeed").value(2.0f).range(1.0f, 10.0f).step(0.1f).setVisible(allowTimer::getValue);

    public AutoDodgeModule() {
        addSettings(allowRotationChange, allowJump, allowTimer, timerSpeed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener movementInputListener = MovementInputEvent.getInstance().subscribe(new Listener<>(100, event -> {
            if (!shouldRun()) return;

            List<Entity> arrows = findFlyingArrows();
            if (arrows.isEmpty()) return;

            HitInfo hitInfo = getInflictedHit(arrows);
            if (hitInfo == null) return;

            DodgePlan dodgePlan = planEvasion(hitInfo);
            if (dodgePlan == null) return;

            // Apply directional input
            event.getDirectionalInput().setForwards(dodgePlan.directionalInput().isForwards());
            event.getDirectionalInput().setBackwards(dodgePlan.directionalInput().isBackwards());
            event.getDirectionalInput().setLeft(dodgePlan.directionalInput().isLeft());
            event.getDirectionalInput().setRight(dodgePlan.directionalInput().isRight());

            // Apply rotation change
            if (dodgePlan.yawChange() != null && allowRotationChange.getValue()) {
                mc.player.setYaw(dodgePlan.yawChange());
            }

            // Schedule jump
            if (dodgePlan.shouldJump() && allowJump.getValue() && mc.player.isOnGround()) {
                EventListener jumpListener = MovementInputEvent.getInstance().subscribe(new Listener<>(1, jumpEvent -> {
                    jumpEvent.setJump(true);
                }));
                addEvents(jumpListener);
            }

            // Apply timer
            if (allowTimer.getValue() && dodgePlan.useTimer()) {
                TimerManager.getInstance().addTimer(timerSpeed.getValue(), TaskPriority.CRITICAL, this, 1);
            }
        }));

        addEvents(movementInputListener);
    }

    private boolean shouldRun() {
        return isEnabled()
                && mc.currentScreen == null
                && !mc.player.isUsingItem();
    }

    private List<Entity> findFlyingArrows() {
        if (mc.world == null) return List.of();

        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(entity -> {
                    if (entity instanceof ArrowEntity) {
                        // In Minecraft 1.21.4, check velocity to determine if arrow is flying
                        return entity.getVelocity().lengthSquared() > 0.001;
                    }
                    if (entity instanceof TridentEntity trident) {
                        return !trident.isNoClip() && !trident.groundCollision;
                    }
                    return false;
                })
                .toList();
    }

    private HitInfo getInflictedHit(List<Entity> arrows) {
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) return null;

        for (int tick = 0; tick < 80; tick++) {
            Vec3d predictedPlayerPos = predictPlayerPosition(tick);

            for (Entity arrow : arrows) {
                Vec3d lastPos = arrow.getPos();
                Vec3d arrowPos = predictArrowPosition(arrow, tick);

                if (isArrowInGround(arrowPos)) continue;

                Box playerHitbox = player.getBoundingBox()
                        .stretch(predictedPlayerPos.subtract(player.getPos()))
                        .expand(SAFE_DISTANCE_WITH_PADDING);

                Vec3d hitPos = playerHitbox.raycast(lastPos, arrowPos).orElse(null);
                if (hitPos != null) {
                    return new HitInfo(tick, arrow, hitPos, lastPos, arrow.getVelocity());
                }
            }
        }

        return null;
    }

    private Vec3d predictPlayerPosition(int ticks) {
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) return Vec3d.ZERO;

        Vec3d pos = player.getPos();
        Vec3d velocity = player.getVelocity();

        // Simple prediction: account for free movement for 2 ticks
        return pos.add(velocity.x * Math.min(2, ticks), 0, velocity.z * Math.min(2, ticks));
    }

    private Vec3d predictArrowPosition(Entity arrow, int ticks) {
        Vec3d pos = arrow.getPos();
        Vec3d velocity = arrow.getVelocity();

        // Simplified arrow prediction (ignoring gravity and drag for brevity)
        return pos.add(velocity.x * ticks, velocity.y * ticks, velocity.z * ticks);
    }

    private boolean isArrowInGround(Vec3d arrowPos) {
        // Simplified check - in real implementation, check actual entity state
        return false;
    }

    private DodgePlan planEvasion(HitInfo hitInfo) {
        Vec3d arrowVelocity2d = hitInfo.arrowVelocity().multiply(1, 0, 1);
        if (arrowVelocity2d.lengthSquared() < 1e-6) {
            return null;
        }

        Vec3d arrowLineStart = hitInfo.prevArrowPos().multiply(1, 0, 1);
        Vec3d arrowLineDir = arrowVelocity2d.normalize();

        Vec3d playerPos2d = mc.player.getPos().multiply(1, 0, 1);
        Vec3d nearestPointOnLine = getNearestPointOnLine(arrowLineStart, arrowLineDir, playerPos2d);
        double distanceToLine = playerPos2d.distanceTo(nearestPointOnLine);

        if (distanceToLine > SAFE_DISTANCE_WITH_PADDING) {
            return null;
        }

        Vec3d optimalDodgePos = findOptimalDodgePosition(arrowLineStart, arrowLineDir, playerPos2d);
        Vec3d positionRelativeToPlayer = optimalDodgePos.subtract(playerPos2d);

        DirectionalInput inputWithoutRotation = getDodgeMovementWithoutAngleChange(positionRelativeToPlayer);

        DodgePlan planWithoutRotation = new DodgePlan(inputWithoutRotation, false, null, false);

        if (distanceToLine > SAFE_DISTANCE) {
            return planWithoutRotation;
        }

        return escalateIfNeeded(planWithoutRotation, positionRelativeToPlayer, optimalDodgePos, hitInfo.tickDelta());
    }

    private DirectionalInput getDodgeMovementWithoutAngleChange(Vec3d positionRelativeToPlayer) {
        double dgs = getDegreesRelativeToView(positionRelativeToPlayer);
        return getDirectionalInputForDegrees(dgs, 20.0f);
    }

    private double getDegreesRelativeToView(Vec3d position) {
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) return 0;

        float yaw = player.getYaw();
        double dx = position.x;
        double dz = position.z;

        double angle = Math.toDegrees(Math.atan2(dz, dx)) - yaw;
        return normalizeAngle(angle);
    }

    private double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    private DirectionalInput getDirectionalInputForDegrees(double angle, float deadAngle) {
        angle = normalizeAngle(angle);

        if (Math.abs(angle) < deadAngle) {
            return new DirectionalInput(true, false, false, false); // Forwards
        } else if (Math.abs(angle - 180) < deadAngle || Math.abs(angle + 180) < deadAngle) {
            return new DirectionalInput(false, true, false, false); // Backwards
        } else if (angle > 0) {
            return new DirectionalInput(false, false, false, true); // Right
        } else {
            return new DirectionalInput(false, false, true, false); // Left
        }
    }

    private Vec3d getNearestPointOnLine(Vec3d lineStart, Vec3d lineDir, Vec3d point) {
        Vec3d toPoint = point.subtract(lineStart);
        double projection = toPoint.dotProduct(lineDir);
        return lineStart.add(lineDir.multiply(projection));
    }

    private Vec3d findOptimalDodgePosition(Vec3d lineStart, Vec3d lineDir, Vec3d playerPos) {
        Vec3d orthoVec = new Vec3d(-lineDir.z, 0, lineDir.x).normalize();

        Vec3d leftBorder = lineStart.add(orthoVec.multiply(SAFE_DISTANCE_WITH_PADDING));
        Vec3d rightBorder = lineStart.subtract(orthoVec.multiply(SAFE_DISTANCE_WITH_PADDING));

        Vec3d leftLine = new Vec3d(leftBorder.x, 0, leftBorder.z);
        Vec3d rightLine = new Vec3d(rightBorder.x, 0, rightBorder.z);

        Vec3d playerPos2d = playerPos;
        Vec3d predictedPos = playerPos2d.add(mc.player.getVelocity().multiply(2, 0, 2));

        Vec3d nearestToLeft = getNearestPointOnLine(leftBorder, lineDir, predictedPos);
        Vec3d nearestToRight = getNearestPointOnLine(rightBorder, lineDir, predictedPos);

        double distToLeft = predictedPos.distanceTo(nearestToLeft);
        double distToRight = predictedPos.distanceTo(nearestToRight);

        // Check walkable distance (simplified)
        if (getWalkableDistance(playerPos, nearestToLeft) < SAFE_DISTANCE) {
            return nearestToRight;
        }
        if (getWalkableDistance(playerPos, nearestToRight) < SAFE_DISTANCE) {
            return nearestToLeft;
        }

        return distToLeft < distToRight - 0.05 ? nearestToLeft : nearestToRight;
    }

    private double getWalkableDistance(Vec3d from, Vec3d to) {
        // Simplified raycast check - in full implementation, check actual blocks
        return from.distanceTo(to);
    }

    private DodgePlan escalateIfNeeded(DodgePlan basePlan, Vec3d positionRelativeToPlayer, 
                                       Vec3d optimalDodgePos, int timeToImpact) {
        double distanceToTravel = optimalDodgePos.length() - (SAFE_DISTANCE_WITH_PADDING - SAFE_DISTANCE);
        double effectivenessLoss = getEffectiveLossByInoptimalAngle(basePlan);
        
        double travelTimeWithRelative = distanceToTravel / (effectivenessLoss * 0.11);

        if (timeToImpact >= travelTimeWithRelative) {
            return basePlan;
        }

        boolean useTimer = shouldUseTimer(distanceToTravel, timeToImpact);

        if (allowRotationChange.getValue()) {
            return planWithRotations(distanceToTravel, timeToImpact, useTimer, optimalDodgePos);
        }

        return new DodgePlan(basePlan.directionalInput(), false, null, useTimer);
    }

    private double getEffectiveLossByInoptimalAngle(DodgePlan plan) {
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) return 1.0;

        DirectionalInput input = plan.directionalInput();
        float movementAngle = getMovementAngle(input);
        
        Vec3d angleVec = new Vec3d(
                -Math.sin(Math.toRadians(movementAngle)),
                0,
                Math.cos(Math.toRadians(movementAngle))
        );

        Vec3d optimalVec = optimalDodgeVector;
        if (optimalVec.lengthSquared() < 1e-6) return 1.0;

        return similarity(angleVec, optimalVec);
    }

    private float getMovementAngle(DirectionalInput input) {
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) return 0;

        float yaw = player.getYaw();
        
        if (input.isForwards() && !input.isLeft() && !input.isRight()) return yaw;
        if (input.isBackwards() && !input.isLeft() && !input.isRight()) return yaw + 180;
        if (!input.isForwards() && !input.isBackwards() && input.isLeft()) return yaw - 90;
        if (!input.isForwards() && !input.isBackwards() && input.isRight()) return yaw + 90;

        if (input.isForwards()) {
            if (input.isLeft()) return yaw - 45;
            if (input.isRight()) return yaw + 45;
        }

        if (input.isBackwards()) {
            if (input.isLeft()) return yaw - 135;
            if (input.isRight()) return yaw + 135;
        }

        return yaw;
    }

    private double similarity(Vec3d a, Vec3d b) {
        double dot = a.dotProduct(b);
        double lenA = a.length();
        double lenB = b.length();
        
        if (lenA < 1e-6 || lenB < 1e-6) return 1.0;
        
        return dot / (lenA * lenB);
    }

    private Vec3d optimalDodgeVector = Vec3d.ZERO;

    private DodgePlan planWithRotations(double distanceToTravel, int timeToImpact, 
                                        boolean useTimer, Vec3d optimalDodgePos) {
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) return new DodgePlan(DirectionalInput.FORWARDS, false, null, useTimer);

        double effectiveVelocity = player.getVelocity().length() * 
                similarity(player.getVelocity(), optimalDodgePos);

        double travelTimeWithRotation = distanceToTravel / 0.13;
        boolean shouldJump = timeToImpact < travelTimeWithRotation && effectiveVelocity > 0.11;

        Vec3d targetPos = player.getPos().add(optimalDodgePos);
        Rotation targetRotation = RotationUtil.rotationAt(targetPos);
        
        RotationManager.getInstance().addRotation(targetRotation, RotationStrategy.TARGET, 
                TaskPriority.CRITICAL, this);

        optimalDodgeVector = optimalDodgePos;

        return new DodgePlan(DirectionalInput.FORWARDS, shouldJump, targetRotation.getYaw(), useTimer);
    }

    private boolean shouldUseTimer(double distanceToTravel, int timeToImpact) {
        double speed = allowRotationChange.getValue() ? 0.155 : 0.11;
        return (distanceToTravel / speed) / (timeToImpact + 1) > 1.6;
    }

    private record HitInfo(int tickDelta, Entity arrowEntity, Vec3d hitPos, 
                           Vec3d prevArrowPos, Vec3d arrowVelocity) {}

    private record DodgePlan(DirectionalInput directionalInput, boolean shouldJump, 
                             Float yawChange, boolean useTimer) {}
}

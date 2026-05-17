package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.other.RotationUpdateEvent;
import worst.woqued.api.event.events.player.world.AttackEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.MultiBooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.system.backend.SharedClass;
import worst.woqued.api.utils.combat.CombatExecutor;
import worst.woqued.api.utils.combat.TargetManager;
import worst.woqued.api.utils.player.DirectionalInput;
import worst.woqued.api.utils.player.MoveUtil;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.misc.AuraUtil;

import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.api.utils.rotation.manager.RotationPlan;
import worst.woqued.api.utils.rotation.manager.RotationStrategy;
import worst.woqued.api.utils.math.TimerUtil;
import worst.woqued.api.utils.rotation.rotations.AresMineRotation;
import worst.woqued.api.utils.rotation.rotations.FunTimeRotation;
import worst.woqued.api.utils.rotation.rotations.MatrixRotation;
import worst.woqued.api.utils.rotation.rotations.MetaHvHRotation;
import worst.woqued.api.utils.rotation.rotations.GrimRotation;
import worst.woqued.api.utils.rotation.rotations.SnapRotation;
import worst.woqued.api.utils.rotation.rotations.PolarRotation;
import worst.woqued.api.utils.rotation.rotations.IntaveRotation;
import worst.woqued.api.utils.rotation.rotations.SpookyDuelsRotation;


import worst.woqued.api.utils.task.TaskPriority;
import worst.woqued.client.features.modules.combat.elytratarget.ElytraTargetModule;
@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    private static final AuraModule instance = new AuraModule();

    public static AuraModule getInstance() {
        return instance;
    }

    private final TargetManager targetManager = new TargetManager();
    public CombatExecutor combatExecutor = new CombatExecutor();
    

    public CombatExecutor getCombatExecutor() {
        return combatExecutor;
    }

    private final FunTimeRotation funTimeRotation = new FunTimeRotation();
    private final GrimRotation grimRotation = new GrimRotation();
    private final AresMineRotation aresMineRotation = new AresMineRotation();
    private final IntaveRotation intaveRotation = new IntaveRotation();
    private final SpookyDuelsRotation spookyDuelsRotation = new SpookyDuelsRotation();
    
    @Getter private final ModeSetting aimMode = new ModeSetting("Aim mode").value("Grim").values(
            "Snap", "Fun Time", "Really World", "Grim", "Intave", "MetaHvH", "Ares Mine", "Fun Sky", "Polar", "Spooky Duels"
    );

    public boolean isFree() {
        return moveCorrection.is("No Correction");
    }

    public static boolean isTargeting() {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan plan = rotationManager.getCurrentRotationPlan();
        return plan != null && plan.provider() instanceof AuraModule && ((AuraModule) plan.provider()).isEnabled();
    }

    @Getter private final ModeSetting clickMode = new ModeSetting("Click mode").value("1.9").values("1.9", "1.8");
    @Getter private final SliderSetting cps = new SliderSetting("CPS").value(9f).range(1f, 20f).step(0.5f).setVisible(() -> clickMode.is("1.8"));

    private final SliderSetting distance = new SliderSetting("Distance").value(3f).range(2.5f, 6f).step(0.1f);
    private final SliderSetting preDistance = new SliderSetting("Pre distance").value(0.3f).range(0f, 3f).step(0.1f);
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(true),
            new BooleanSetting("Animals").value(true)
    );

    public final MultiBooleanSetting options = combatExecutor.options();
    private final BooleanSetting clientLook = new BooleanSetting("Client look").value(false);
    private final BooleanSetting elytraOverride = new BooleanSetting("Elytra override").value(false);
    private final SliderSetting elytraDistance = new SliderSetting("Elytra distance").value(4f).range(2.5f, 6f).step(0.1f).setVisible(elytraOverride::getValue);
    private final SliderSetting elytraPreDistance = new SliderSetting("Elytra pre distance").value(16f).range(0f, 32f).step(0.1f).setVisible(elytraOverride::getValue);
    
    public final ModeSetting moveCorrection = new ModeSetting("Move Correction").value("Focus").values("Focus", "No Correction");

    public LivingEntity target;
    private LivingEntity previousTarget = null;
    private long lastSecondTick = 0;

    public AuraModule() {
        addSettings(aimMode, clickMode, cps, distance, preDistance, targets, options, clientLook,
                elytraOverride, elytraDistance, elytraPreDistance, moveCorrection
        );
    }

    public float getPreDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraPreDistance.getValue() : preDistance.getValue();
    }

    public float getAttackDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraDistance.getValue() : distance.getValue();
    }

    @Override
    public void onDisable() {
        targetManager.releaseTarget();
        target = null;
        previousTarget = null;
        RotationManager.getInstance().startReturning();
    }
    @Override
    public void onEnable() {
        targetManager.releaseTarget();
        target = null;
    }

    @Override
    public void onEvent() {
        EventListener eventUpdate = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            updateEventHandler();
        }));

        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            postRotMoveEventHandler();
        }));

        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            AuraUtil.onAttack(aimMode.getValue());
        }));
        
        EventListener movementCorrectionEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            applyMovementCorrection();
        }));
        
        addEvents(eventUpdate, rotationUpdateEvent, attackEvent, movementCorrectionEvent);
    }

    private void postRotMoveEventHandler() {
        if (target == null) {
            return;
        }
        Vec3d attackVector = getTargetVector(target);
        Rotation rotation = RotationUtil.fromVec3d(attackVector.subtract(mc.player.getEyePos()));
        rotateToTarget(target, attackVector, rotation);
    }

    private void updateEventHandler() {
        target = updateTarget();

        previousTarget = target;

        if (target == null) return;

        if (RotationUtil.getSpot(target).distanceTo(mc.player.getEyePos()) > getAttackDistance() + getPreDistance()) {
            targetManager.releaseTarget();
            return;
        }

        if (target != null) {
            attackTarget(target);
        }
    }

    /**
     * МОДИФИЦИРОВАННЫЙ МЕТОД: Теперь проверяет AntiBot
     */
    private LivingEntity updateTarget() {
        TargetManager.EntityFilter filter = new TargetManager.EntityFilter(targets.getList());
        targetManager.searchTargets(mc.world.getEntities(), getAttackDistance() + getPreDistance());

        // Добавляем проверку AntiBot в валидацию
        targetManager.validateTarget(entity -> {
            // Сначала базовая проверка (игроки/мобы/животные)
            if (!filter.isValid(entity)) return false;

            // Если AntiBot включен и считает сущность ботом - пропускаем
            if (AntiBotModule.getInstance().isEnabled() && AntiBotModule.getInstance().isBot(entity)) {
                return false;
            }

            return true;
        });

        return targetManager.getCurrentTarget();
    }

    private void attackTarget(LivingEntity target) {
        if (clickMode.is("1.8") && !combatExecutor.combatManager().clickScheduler().isCooldownComplete()) {
            return;
        }

        combatExecutor.combatManager().configurable(
                new CombatExecutor.CombatConfigurable(
                        target,
                        RotationManager.getInstance().getRotation(),
                        distance.getValue(),
                        options.getList()
                )
        );

        if (mc.player.getEyePos().distanceTo(
                RotationUtil.rayCastBox(target, getTargetVector(target))
        ) > getAttackDistance()) {
            return;
        }

        combatExecutor.performAttack();

        if (clickMode.is("1.8")) {
            long delay = (long) (1000.0 / cps.getValue());
            combatExecutor.combatManager().clickScheduler().recalculate(delay);
        }
    }

    private void rotateToTarget(LivingEntity target, Vec3d targetVec, Rotation rotation) {
        if (combatExecutor.combatManager().configurable() == null) return;
        
        RotationStrategy configurable = new RotationStrategy(getRotationMode(), moveCorrection.is("Focus"), moveCorrection.is("No Correction")).clientLook(clientLook.getValue());

        boolean noHitRule = (!combatExecutor.combatManager().canAttack());

        if (usingElytraTarget() && ElytraTargetModule.getInstance().elytraRotationProcessor.customRotations.getValue()) return;

        if (noHitRule && aimMode.is("Snap")) {
            if (!moveCorrection.is("Focus"))
                return;
            else rotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        RotationManager.getInstance().addRotation(new Rotation.VecRotation(rotation, targetVec), target, configurable, TaskPriority.HIGH, this);
    }

    private final MetaHvHRotation metaHvHRotation = new MetaHvHRotation();
    private final PolarRotation polarRotation = new PolarRotation();

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "Fun Time" -> funTimeRotation;
            case "Snap" -> new SnapRotation();
            case "Really World" -> new MatrixRotation();
            case "Grim", "Fun Sky" -> grimRotation;
            case "Intave" -> intaveRotation;
            case "MetaHvH" -> metaHvHRotation;
            case "Ares Mine" -> aresMineRotation;
            case "Polar" -> polarRotation;
            case "Spooky Duels" -> spookyDuelsRotation;
            default -> new SnapRotation();
        };
    }
    private Vec3d getTargetVector(LivingEntity target) {
        if (target == null) return Vec3d.ZERO;
        if (usingElytraTarget()) {
            return ElytraTargetModule.getInstance().elytraRotationProcessor.getPredictedPos(target);
        }
        return target.getEyePos();
    }

    private boolean usingElytraTarget() {
        return target != null && ElytraTargetModule.getInstance().elytraRotationProcessor.using();
    }

    /**
     * Коррекция движения при наведении на цель (Focus режим)
     * Движение всегда направлено к цели, независимо от взгляда игрока
     */
    public DirectionalInput transformDirectionForTargeting(DirectionalInput input) {
        net.minecraft.client.network.ClientPlayerEntity player = SharedClass.player();
        if (player == null || target == null) {
            return input;
        }

        float z = KeyboardInput.getMovementMultiplier(input.isForwards(), input.isBackwards());
        float x = KeyboardInput.getMovementMultiplier(input.isLeft(), input.isRight());

        if (z == 0 && x == 0) {
            return input;
        }

        Vec3d aimPoint = getTargetVector(target);
        double deltaX = aimPoint.x - player.getPos().x;
        double deltaZ = aimPoint.z - player.getPos().z;

        double angleToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
        angleToTarget = MathHelper.wrapDegrees(angleToTarget);

        float yaw = player.getYaw();

        float relativeAngle = (float)(angleToTarget - yaw);
        relativeAngle = MathHelper.wrapDegrees(relativeAngle);

        float moveForward = MathHelper.cos(relativeAngle * 0.017453292f);
        float moveStrafe = -MathHelper.sin(relativeAngle * 0.017453292f);

        int forward = Math.round(moveForward);
        int strafe = Math.round(moveStrafe);

        boolean movingBackwards = input.isBackwards();
        if (movingBackwards && forward > 0) {
            forward = -Math.abs(forward);
        } else if (!movingBackwards && forward < 0) {
            forward = Math.abs(forward);
        }

        return new DirectionalInput(forward, strafe);
    }

public DirectionalInput transformDirectionForFree(DirectionalInput input) {
        return input;
    }

    /**
     * Основная точка входа для коррекции движения
     */
    public DirectionalInput transformDirection(DirectionalInput input, RotationPlan rotationPlan, Rotation rotation) {
        if (rotationPlan == null || rotation == null) {
            return input;
        }

        if (!rotationPlan.moveCorrection()) {
            return input;
        }

        if (moveCorrection.is("Focus")) {
            return transformDirectionForTargeting(input);
        }

        if (moveCorrection.is("No Correction")) {
            return transformDirectionForFree(input);
        }

        return input;
    }

    public void applyMovementCorrection() {
    }
}
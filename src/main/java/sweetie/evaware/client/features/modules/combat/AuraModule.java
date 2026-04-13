package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.other.RotationUpdateEvent;
import sweetie.evaware.api.event.events.player.world.AttackEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.combat.CombatExecutor;
import sweetie.evaware.api.utils.combat.TargetManager;
import sweetie.evaware.api.utils.neuro.AIPredictor;
import sweetie.evaware.api.utils.rotation.misc.AuraUtil;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;
import sweetie.evaware.api.utils.rotation.manager.RotationStrategy;
import sweetie.evaware.api.utils.rotation.rotations.*;
import sweetie.evaware.api.utils.task.TaskPriority;
import sweetie.evaware.client.features.modules.combat.elytratarget.ElytraTargetModule;
import sweetie.evaware.client.features.modules.movement.MoveFixModule;
@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    @Getter private static final AuraModule instance = new AuraModule();

    private final AIPredictor predictor = new AIPredictor();
    private final TargetManager targetManager = new TargetManager();
    public final CombatExecutor combatExecutor = new CombatExecutor();
    private final SlothRotation slothRotation = new SlothRotation();
    private final FunTimeRotation funTimeRotation = new FunTimeRotation();
    @Getter private final ModeSetting aimMode = new ModeSetting("Aim mode").value("Grim").values(
            "Grim", "Ft snap", "Really World", "Sloth"
    ).onAction(() -> {
        if (getAimMode().is("Sloth")) {
            if (predictor.isLoaded()) {
                predictor.close();
            }
            loadModel();
        } else {
            predictor.close();
        }
    });

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

    public LivingEntity target;
    private LivingEntity previousTarget = null;

    public AuraModule() {
        addSettings(aimMode, distance, preDistance, targets, options, clientLook,
                elytraOverride, elytraDistance, elytraPreDistance
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
        predictor.close();
        slothRotation.reset();
        if (aimMode.is("Ft snap")) {
            funTimeRotation.startRelease();
        }
        // Сбрасываем ротацию при отключении, чтобы сервер не застревал
        releaseRotationLock();
    }
    @Override
    public void onEnable() {
        targetManager.releaseTarget();
        target = null;
        if (aimMode.is("Sloth") && !predictor.isLoaded()) {
            loadModel();
        }
    }

    public void loadModel() {
        predictor.loadModel("Default");
    }

    @Override
    public void onEvent() {
        predictor.onEvent();

        EventListener eventUpdate = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            updateEventHandler();
        }));

        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            postRotMoveEventHandler();
        }));

        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (aimMode.is("Sloth")) {
                slothRotation.onAttack();
            }
            AuraUtil.onAttack(aimMode.getValue());
        }));
        addEvents(predictor.getEventListeners());
        addEvents(eventUpdate, rotationUpdateEvent, attackEvent);
    }

    private void postRotMoveEventHandler() {
        if (target == null) {
            // Если цель потеряна - сбрасываем ротацию
            if (previousTarget != null) {
                releaseRotationLock();
            }
            return;
        }
        Vec3d attackVector = getTargetVector(target);
        Rotation rotation = RotationUtil.fromVec3d(attackVector.subtract(mc.player.getEyePos()));
        rotateToTarget(target, attackVector, rotation);
    }

    private void updateEventHandler() {
        target = updateTarget();

        // Отслеживаем потерю цели
        if (target == null && previousTarget != null) {
            if (aimMode.is("Ft snap")) {
                funTimeRotation.startRelease();
            }
            releaseRotationLock();
        }
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
        ) > getAttackDistance()) return;

        combatExecutor.performAttack();
    }

    private void rotateToTarget(LivingEntity target, Vec3d targetVec, Rotation rotation) {
        RotationStrategy configurable = new RotationStrategy(getRotationMode(),
                MoveFixModule.enabled(), MoveFixModule.isFree()).clientLook(clientLook.getValue());

        boolean noHitRule = (!combatExecutor.combatManager().canAttack());

        if (usingElytraTarget() && ElytraTargetModule.getInstance().elytraRotationProcessor.customRotations.getValue()) return;

        if (noHitRule && aimMode.is("Snap")) {
            if (!(MoveFixModule.getInstance().isEnabled() && MoveFixModule.getInstance().targeting.getValue()))
                return;
            else rotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        RotationManager.getInstance().addRotation(new Rotation.VecRotation(rotation, targetVec), target, configurable, TaskPriority.HIGH, this);
    }

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "Ft snap" -> funTimeRotation;
            case "Grim" -> new SnapRotation();
            case "Really World" -> new MatrixRotation();
            case "Sloth" -> slothRotation;
            default -> new SnapRotation();
        };
    }
    private Vec3d getTargetVector(LivingEntity target) {
        if (target == null) return Vec3d.ZERO;
        if (usingElytraTarget()) {
            return ElytraTargetModule.getInstance().elytraRotationProcessor.getPredictedPos(target);
        }
        return AuraUtil.getAimpoint(target, aimMode.getValue());
    }

    private boolean usingElytraTarget() {
        return target != null && ElytraTargetModule.getInstance().elytraRotationProcessor.using();
    }

    /**
     * Сбрасывает блокировку ротации, когда цель потеряна.
     * Позволяет персонажу вернуться к нормальному управлению камерой.
     */
    private void releaseRotationLock() {
        RotationManager.getInstance().getRotationPlanRequestProcessor().clearTasksForProvider(this);
        RotationManager.getInstance().setLastRotationPlan(null);
        RotationManager.getInstance().setRotation(null);
        
        // Сбрасываем визуальную ротацию персонажа к текущей клиентской
        if (mc.player != null) {
            float clientYaw = mc.player.getYaw();
            float clientPitch = mc.player.getPitch();
            mc.player.setYaw(clientYaw);
            mc.player.setPitch(clientPitch);
            mc.player.setHeadYaw(clientYaw);
            mc.player.setBodyYaw(clientYaw);
        }
        
        // Синхронизируем серверную ротацию с клиентской
        RotationManager.getInstance().forceSyncToServer();
    }
}
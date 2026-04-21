package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
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
import worst.woqued.api.utils.combat.CombatExecutor;
import worst.woqued.api.utils.combat.TargetManager;
import worst.woqued.api.utils.neuro.AIPredictor;
import worst.woqued.api.utils.rotation.misc.AuraUtil;
import worst.woqued.api.utils.rotation.RotationUtil;
import worst.woqued.api.utils.rotation.manager.Rotation;
import worst.woqued.api.utils.rotation.manager.RotationManager;
import worst.woqued.api.utils.rotation.manager.RotationMode;
import worst.woqued.api.utils.rotation.manager.RotationStrategy;
import worst.woqued.api.utils.rotation.rotations.*;
import worst.woqued.api.utils.rotation.rotations.*;
import worst.woqued.api.utils.task.TaskPriority;
import worst.woqued.client.features.modules.combat.elytratarget.ElytraTargetModule;
import worst.woqued.client.features.modules.movement.MoveFixModule;
@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    private static final AuraModule instance = new AuraModule();

    public static AuraModule getInstance() {
        return instance;
    }

    private final AIPredictor predictor = new AIPredictor();
    private final TargetManager targetManager = new TargetManager();
    public CombatExecutor combatExecutor = new CombatExecutor();

    public CombatExecutor getCombatExecutor() {
        return combatExecutor;
    }

    private final FunTimeRotation funTimeRotation = new FunTimeRotation();
    private final PolarRotation polarRotation = new PolarRotation();
    @Getter private final ModeSetting aimMode = new ModeSetting("Aim mode").value("Grim").values(
            "Grim", "Ft snap", "Really World", "Polar", "MetaHvH"
    );

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
        // Плавное возвращение камеры при отключении
        RotationManager.getInstance().startReturning();
    }
    @Override
    public void onEnable() {
        targetManager.releaseTarget();
        target = null;
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
            AuraUtil.onAttack(aimMode.getValue());
        }));
        addEvents(predictor.getEventListeners());
        addEvents(eventUpdate, rotationUpdateEvent, attackEvent);
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
            // Отводка когда не хватает дистанции для удара но таргет не потерян (аналогично Ft snap logic)
            if (aimMode.is("Ft snap")) {
                funTimeRotation.startRelease();
            }
            return;
        }

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

    private final MetaHvHRotation metaHvHRotation = new MetaHvHRotation();

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "Ft snap" -> funTimeRotation;
            case "Grim" -> new SnapRotation();
            case "Really World" -> new MatrixRotation();
            case "Polar" -> polarRotation;
            case "MetaHvH" -> metaHvHRotation;
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
}
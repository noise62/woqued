package sweetie.evaware.api.utils.rotation.manager;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.*;
import sweetie.evaware.api.event.events.other.RotationUpdateEvent;
import sweetie.evaware.api.event.events.player.other.MovementInputEvent;
import sweetie.evaware.api.event.events.player.other.PostRotationMovementInputEvent;
import sweetie.evaware.api.event.events.player.move.VelocityEvent;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.task.TaskPriority;
import sweetie.evaware.api.utils.task.TaskProcessor;

@Getter
@Setter
public class RotationManager implements QuickImports {
    @Getter private final static RotationManager instance = new RotationManager();

    private RotationPlan lastRotationPlan;
    private final TaskProcessor<RotationPlan> rotationPlanRequestProcessor = new TaskProcessor<>();
    private Rotation currentRotation;
    private Rotation previousRotation;
    private Rotation serverRotation = Rotation.DEFAULT;
    private boolean returning = false;

    public void load() {
        VelocityEvent.getInstance().subscribe(new Listener<>(event -> {
            if (getCurrentRotationPlan() != null && getCurrentRotationPlan().moveCorrection()) {
                event.setVelocity(Entity.movementInputToVelocity(event.getMovementInput(), event.getSpeed(), getRotation().getYaw()));
            }
        }));

        PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isSend()) {
                Rotation rotation;

                if (event.packet() instanceof PlayerMoveC2SPacket packet) {
                    if (packet.changesLook()) {
                        rotation = new Rotation(packet.getYaw(1f), packet.getPitch(1f));
                    } else {
                        return;
                    }
                } else if (event.packet() instanceof PlayerPositionLookS2CPacket packet) {
                    rotation = new Rotation(packet.change().yaw(), packet.change().pitch());
                } else {
                    return;
                }

                if (!PacketEvent.getInstance().isCancel()) {
                    serverRotation = rotation;
                }
            }
        }));

        MovementInputEvent.getInstance().subscribe(new Listener<>(event -> {
            PostRotationMovementInputEvent.getInstance().call();
        }));

        GameLoopEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            RotationUpdateEvent.getInstance().call();
            update();
        }));
    }

    public void setRotation(Rotation value) {
        previousRotation = (value == null) ? (currentRotation != null ? currentRotation : mc.player != null ? new Rotation(mc.player.getYaw(), mc.player.getPitch()) : Rotation.DEFAULT) : currentRotation;
        currentRotation = value;
    }

    public Rotation getRotation() {
        if (mc.player == null) return Rotation.DEFAULT;
        return currentRotation != null ? currentRotation : RotationUtil.fromVec2f(mc.player.getRotationClient());
    }

    public Rotation getPreviousRotation() {
        if (mc.player == null) return Rotation.DEFAULT;
        return previousRotation != null ? previousRotation : RotationUtil.fromVec2f(mc.player.getRotationClient());
    }

    public RotationPlan getCurrentRotationPlan() {
        return rotationPlanRequestProcessor.fetchActiveTaskValue() != null ? rotationPlanRequestProcessor.fetchActiveTaskValue() : lastRotationPlan;
    }

    public void addRotation(Rotation.VecRotation vecRotation, LivingEntity entity, RotationStrategy configurable, TaskPriority requestPriority, Module provider) {
        addRotation(configurable.createRotationPlan(vecRotation.rotation(), vecRotation.vec(), entity, provider), requestPriority, provider);
    }

    public void addRotation(Rotation rotation, RotationStrategy configurable, TaskPriority requestPriority, Module provider) {
        addRotation(configurable.createRotationPlan(rotation, provider), requestPriority, provider);
    }

    private void addRotation(RotationPlan plan, TaskPriority requestPriority, Module provider) {
        rotationPlanRequestProcessor.addTask(new TaskProcessor.Task<>(plan.ticksUntilReset(), requestPriority.getPriority(), provider, plan));
    }

    private void update() {
        RotationPlan activePlan = getCurrentRotationPlan();

        if (activePlan == null) {
            if (currentRotation != null && returning) {
                Rotation cameraRotation = RotationUtil.fromVec2f(mc.player.getRotationClient());
                double diff = computeRotationDifference(currentRotation, cameraRotation);

                if (diff < 0.5) {
                    setRotation(null);
                    lastRotationPlan = null;
                    returning = false;
                } else {
                    float speed = 0.25f;
                    float distanceFactor = Math.min(1.0f, (float) diff / 30.0f);
                    speed = speed + (0.4f * distanceFactor);

                    float yawDiff = MathHelper.wrapDegrees(cameraRotation.getYaw() - currentRotation.getYaw());
                    float newYaw = currentRotation.getYaw() + yawDiff * speed;
                    float newPitch = MathHelper.lerp(speed, currentRotation.getPitch(), cameraRotation.getPitch());

                    setRotation(new Rotation(newYaw, newPitch).adjustSensitivity());
                }
            }
            return;
        }

        returning = false;

        Rotation clientAngle = RotationUtil.fromVec2f(mc.player.getRotationClient());

        if (lastRotationPlan != null) {
            double differenceFromCurrentToPlayer = computeRotationDifference(serverRotation, clientAngle);
            if (rotationPlanRequestProcessor.getResetTickCounter() >= activePlan.ticksUntilReset() && differenceFromCurrentToPlayer < activePlan.resetThreshold()) {
                setRotation(null);
                lastRotationPlan = null;
                rotationPlanRequestProcessor.reset();
                return;
            }
        }

        Rotation newRotation = activePlan.nextRotation(currentRotation != null ? currentRotation : clientAngle, rotationPlanRequestProcessor.fetchActiveTaskValue() == null).adjustSensitivity();
        setRotation(newRotation);

        if (activePlan.clientLook()) {
            mc.player.setYaw(newRotation.getYaw());
            mc.player.setPitch(newRotation.getPitch());
        }

        lastRotationPlan = activePlan;

        rotationPlanRequestProcessor.tick(1);
    }

    private double computeRotationDifference(Rotation a, Rotation b) {
        return Math.hypot(MathHelper.abs(computeAngleDifference(a.getYaw(), b.getYaw())), MathHelper.abs(a.getPitch() - b.getPitch()));
    }

    private float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

    /**
     * Принудительно синхронизирует серверную ротацию с текущей ротацией игрока.
     * Отправляет пакет PlayerMoveC2SPacket.LookAndOnGround на сервер.
     */
    public void forceSyncToServer() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    false
            ));
        }
    }

    /**
     * Запускает плавное возвращение камеры к нормальному управлению.
     * В Rich-Modern startReturning() пустой - плавный возврат происходит
     * автоматически через isResetting в nextRotation когда задача истекает.
     */
    public void startReturning() {
        // Ничего не делаем - плавный возврат обрабатывается через isResetting
    }

    /**
     * Сбрасывает все задачи ротации и возвращает камеру.
     */
    public void clear() {
        rotationPlanRequestProcessor.clearAll();
    }
}

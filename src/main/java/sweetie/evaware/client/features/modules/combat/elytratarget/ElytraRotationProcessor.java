package sweetie.evaware.client.features.modules.combat.elytratarget;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.api.system.backend.Configurable;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationStrategy;
import sweetie.evaware.api.utils.task.TaskPriority;
import sweetie.evaware.client.features.modules.combat.AuraModule;

import java.util.function.Supplier;

import static java.lang.Math.*;

public class ElytraRotationProcessor extends Configurable implements QuickImports {
    private final ElytraTargetModule elytraTargetModule;
    private TargetPosition targetPositionMode = TargetPosition.CENTER;

    private static final float BASE_YAW_SPEED = 45.0f;
    private static final float BASE_PITCH_SPEED = 35.0f;
    private static final int IDEAL_DISTANCE = 10;

    private final Supplier<TargetMovementPrediction> predict = TargetMovementPrediction::new;

    public final BooleanSetting customRotations = new BooleanSetting("Custom rotations").value(true);
    private final BooleanSetting sharpRotations = new BooleanSetting("Sharp").value(false).setVisible(customRotations::getValue);
    private final BooleanSetting autoDistance = new BooleanSetting("Auto distance").value(true);

    @Getter private final ModeSetting rotateAt = new ModeSetting("Rotate at")
            .value(targetPositionMode)
            .values(TargetPosition.values())
            .onAction(() -> {
                targetPositionMode = Choice.getChoiceByName(getRotateAt().getValue(), TargetPosition.values());
            });

    public ElytraRotationProcessor(ElytraTargetModule elytraTargetModule) {
        this.elytraTargetModule = elytraTargetModule;
        addSettings(customRotations, sharpRotations, autoDistance, rotateAt);
        addSettings(predict.get().getSettings());
    }

    public boolean using() {
        return elytraTargetModule.isEnabled() && AuraModule.getInstance().target != null && mc.player.isGliding();
    }

    public void processRotation() {
        LivingEntity target = AuraModule.getInstance().target;
        if (!using() || !customRotations.getValue() || target == null) return;
        Rotation targetRotation = calculateRotation(target);

        Rotation currentRotation = RotationManager.getInstance().getCurrentRotation();
        if (currentRotation == null) {
            currentRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        Rotation processedRotation = process(currentRotation, targetRotation);

        RotationManager.getInstance().addRotation(
                new Rotation.VecRotation(processedRotation, getPredictedPos(target)),
                target,
                RotationStrategy.TARGET, TaskPriority.HIGH,
                elytraTargetModule
        );
    }

    private Rotation process(Rotation currentRotation, Rotation targetRotation) {
        Rotation delta = currentRotation.rotationDeltaTo(targetRotation);

        float deltaYaw = delta.getYaw();
        float deltaPitch = delta.getPitch();
        float difference = (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        long currentTime = System.currentTimeMillis();
        boolean shouldBoost = sin(currentTime / 300.0) > 0.8;
        boolean isTargetBehind = abs(deltaYaw) > 90.0f;

        float speedMultiplier = shouldBoost ? 2.0f : 1.2f;
        float smoothBoost = shouldBoost
                ? (float) (sin((currentTime % 360) / 300.0f * Math.PI) * 0.8f + 1.2f)
                : 1.2f;

        float backTargetMultiplier = isTargetBehind
                ? (float) (2.2f * sin(currentTime / 150.0) * 0.2 + 1.0)
                : 1.2f;

        float speed = speedMultiplier * smoothBoost;

        float yawSpeed = getBaseYawSpeed() * speed * backTargetMultiplier;
        float pitchSpeed = getBasePitchSpeed() * speed;

        float microAdjustment = (float) (sin(currentTime / 80.0) * 0.08 + cos(currentTime / 120.0) * 0.05);

        float moveYaw = MathHelper.clamp(deltaYaw, -yawSpeed, yawSpeed);
        float movePitch = MathHelper.clamp(deltaPitch, -pitchSpeed, pitchSpeed);

        if (difference < 5.0f) {
            moveYaw += microAdjustment * 0.2f;
            movePitch += microAdjustment * 0.8f;
        }

        return new Rotation(
                currentRotation.getYaw() + moveYaw,
                MathHelper.clamp(currentRotation.getPitch() + movePitch, -90.0f, 90.0f)
        );
    }

    public Rotation calculateRotation(LivingEntity target) {
        Vec3d targetPos = getPredictedPos(target);

        if (autoDistance.getValue()) {
            Vec3d playerPos = mc.player.getPos();
            Vec3d direction = targetPos.subtract(playerPos).normalize();
            double distance = playerPos.squaredDistanceTo(direction);

            if (distance < IDEAL_DISTANCE * IDEAL_DISTANCE) {
                targetPos = targetPos.subtract(direction.multiply(IDEAL_DISTANCE - distance));
            }
        }

        return RotationUtil.rotationAt(targetPos);
    }

    public Vec3d getPredictedPos(LivingEntity target) {
        return predict.get().predictPosition(target, targetPositionMode.getPosition(target))
                .add(getRandomDirectionVector().multiply(4.0));
    }

    private float getBaseYawSpeed() {
        return (sharpRotations.getValue() ? BASE_YAW_SPEED * 1.5f : BASE_YAW_SPEED) / 3f;
    }

    private float getBasePitchSpeed() {
        return (sharpRotations.getValue() ? BASE_PITCH_SPEED * 1.5f : BASE_PITCH_SPEED) / 3f;
    }

    private Vec3d getRandomDirectionVector() {
        double t = System.currentTimeMillis() / 1000.0;
        return new Vec3d(
                sin(t * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02,
                sin(t * 2.2) * 0.03 + (Math.random() - 0.5) * 0.015,
                cos(t * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02
        );
    }
}

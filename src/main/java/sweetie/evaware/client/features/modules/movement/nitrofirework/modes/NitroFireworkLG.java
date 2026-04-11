package sweetie.evaware.client.features.modules.movement.nitrofirework.modes;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.module.setting.*;
import sweetie.evaware.api.system.backend.Pair;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.client.features.modules.combat.AuraModule;
import sweetie.evaware.client.features.modules.combat.elytratarget.ElytraTargetModule;
import sweetie.evaware.client.features.modules.movement.nitrofirework.NitroFireworkMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class NitroFireworkLG extends NitroFireworkMode {
    @Override
    public String getName() {
        return "Grim";
    }

    private final ModeSetting mode = new ModeSetting("Grim boost").value("Legends Grief").values("Legends Grief", "Really World");
    private final SliderSetting inTargetValue = new SliderSetting("In target value").value(3f).range(0.1f, 6f).step(0.1f);
    private final MultiBooleanSetting options = new MultiBooleanSetting("Options").value(
            new BooleanSetting("Vertical boost").value(true),
            new BooleanSetting("Extra speed").value(false)
    );
    private final BooleanSetting speedPlus = new BooleanSetting("Speed plus").value(true).setVisible(() -> !mode.is("Custom") && options.isEnabled("Extra speed"));

    private final BooleanSetting distanceBasedSpeed = new BooleanSetting("Distance based speed").value(true);


    public NitroFireworkLG(Supplier<Boolean> condition) {
       addSettings(mode, inTargetValue, options, speedPlus, distanceBasedSpeed);
       getSettings().forEach(setting -> setting.setVisible(condition));
    }

    @Override
    public Pair<Float, Float> velocityValues() {
        boolean inTarget = false;
        AuraModule aura = AuraModule.getInstance();
        ElytraTargetModule elytraTarget = ElytraTargetModule.getInstance();
        Vec3d targetVector = null;
        if (aura.target != null) {
            Box targetBB = aura.target.getBoundingBox();
            Vec3d bestCandidate = new Vec3d(
                    (targetBB.minX + targetBB.maxX) / 2.0,
                    (targetBB.minY + targetBB.maxY) / 2.0,
                    (targetBB.minZ + targetBB.maxZ) / 2.0
            );

            if (aura.target.isGliding() && elytraTarget.isEnabled()) {
                Vec3d predictedPos = elytraTarget.elytraRotationProcessor.getPredictedPos(aura.target);
                if (predictedPos != null) {
                    bestCandidate = predictedPos;
                }

                if (distanceBasedSpeed.getValue()) {
                    bestCandidate.add(aura.target.getVelocity().multiply(0.5));
                }
            }

            targetVector = bestCandidate;

            if (mc.player.getEyePos().distanceTo(bestCandidate) < inTargetValue.getValue()) {
                inTarget = true;
            }
        }

        Rotation rotation = RotationManager.getInstance().getCurrentRotation();
        float yaw = rotation != null ? rotation.getYaw() : mc.player.getYaw();
        float pitch = rotation != null ? rotation.getPitch() : mc.player.getPitch();

        float speed = 1.615f;
        int[] yawAngles = {45, 135, 225, 315};
        int[][] yawRanges = {
                {13, 14, 18, 19, 20, 22, 23, 25, 26, 29},
                {13, 14, 17, 18, 19, 23, 24, 28},
                {13, 14, 17, 18, 19, 23, 24, 28},
                {13, 14, 17, 18, 19, 23, 24, 28}
        };
        float[][] yawSpeeds;
        if (mode.is("Legends Grief")) {
            yawSpeeds = new float[][]{
                    {2.0255f,2.065f, 2.045f, 2.035f, 2.025f, 1.965f, 1.78f, 1.76f, 1.73f, 1.72f, 1.7f},
                    {2.0255f, 2.065f, 2.045f, 2.035f, 2.025f, 1.965f, 1.77f, 1.75f, 1.7f},
                    {2.0255f, 2.065f, 2.045f, 2.035f, 2.025f, 1.965f, 1.77f, 1.75f, 1.7f},
                    {2.0255f, 2.065f, 2.045f, 2.035f, 2.025f, 1.965f, 1.77f, 1.75f, 1.7f}
            };
        } else {
            yawSpeeds = new float[][]{
                    {1.805f, 1.805f, 1.87f, 1.85f, 1.83f, 1.8f, 1.78f, 1.76f, 1.73f, 1.72f, 1.7f},
                    {1.805f, 1.805f, 1.87f, 1.85f, 1.82f, 1.8f, 1.77f, 1.75f, 1.7f},
                    {1.805f, 1.805f, 1.87f, 1.85f, 1.82f, 1.8f, 1.77f, 1.75f, 1.7f},
                    {1.805f, 1.805f, 1.87f, 1.85f, 1.82f, 1.8f, 1.77f, 1.75f, 1.7f}
            };
        }

        for (int i = 0; i < yawAngles.length; i++) {
            int currentYaw = yawAngles[i];
            int[] ranges = yawRanges[i];
            float[] speeds = yawSpeeds[i];

            if (isYawInRange(yaw, currentYaw, ranges[0])) {
                if (pitch >= 1 && pitch <= 90) {
                    speed = (speeds[0]);
                } else {
                    speed = (speeds[1]);
                }
            }

            for (int j = 1; j < ranges.length; j++) {
                if (isYawInRange(yaw, currentYaw, ranges[j]) && !isYawInRange(yaw, currentYaw, ranges[j - 1])) {
                    speed = (speeds[j + 1]);
                }
            }
        }

        if (pitch <= -30 || pitch >= 30) {
            speed = 1.615f;
        }

        if (pitch <= -80 || pitch >= 80) {
            speed = 1.715f;
        }

        boolean isDiagonalYaw = isYawInRange(yaw, 45.0f, 20.0f) ||
                isYawInRange(yaw, 135.0f, 20.0f) ||
                isYawInRange(yaw, 225.0f, 20.0f) ||
                isYawInRange(yaw, 315.0f, 20.0f);

        if (!isDiagonalYaw && ((pitch >= 15.0f && pitch <= 35.0f) || (pitch <= -15.0f && pitch >= -35.0f))) {
            speed = 1.7f;
        }
        if (options.isEnabled("Extra speed")){
            if (((pitch >= 35.0f && pitch <= 60f) || (pitch <= -35.0f && pitch >= -52.0f))) {
                if (speedPlus.getValue()) {
                    speed = 2.15f;
                }
                else{
                    speed = 1.95f;
                }
            }
        }

        float targetSpeed = 1.5f;

        if (distanceBasedSpeed.getValue() && targetVector != null) {
            float distanceFactor = (float) (mc.player.getEyePos().distanceTo(targetVector) / AuraModule.getInstance().getAttackDistance());
            targetSpeed = Math.max(1.3f, speed * distanceFactor);
        }

        float speedXZ = inTarget ? targetSpeed : speed;

        return new Pair<>(speedXZ, options.isEnabled("Vertical boost") ? speedXZ : 1.5f);
    }

    public boolean isYawInRange(float yaw, float firstValue, float radiusValue) {
        yaw = (yaw % 360 + 360) % 360;
        firstValue = (firstValue % 360 + 360) % 360;

        float minValue = (firstValue - radiusValue + 360) % 360;
        float maxValue = (firstValue + radiusValue) % 360;

        if (minValue < maxValue) {
            return yaw >= minValue && yaw <= maxValue;
        } else {
            return yaw >= minValue || yaw <= maxValue;
        }
    }
}

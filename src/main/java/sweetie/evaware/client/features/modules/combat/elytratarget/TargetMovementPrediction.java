package sweetie.evaware.client.features.modules.combat.elytratarget;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.Setting;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.api.system.backend.Configurable;
import sweetie.evaware.api.utils.math.MathUtil;

import java.util.function.Supplier;

public class TargetMovementPrediction extends Configurable {
    private PredictMode predictMode = PredictMode.SIMPLE;

    private final BooleanSetting prediction = new BooleanSetting("Prediction").value(true);
    private final Supplier<Boolean> isPredict = prediction::getValue;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(PredictMode.SIMPLE).values(
            PredictMode.values()
    ).onAction(() -> {
        predictMode = Choice.getChoiceByName(getMode().getValue(), PredictMode.values());
    });

    private final BooleanSetting glidingOnly = new BooleanSetting("Gliding only").value(true);

    private final SliderSetting multiplier = new SliderSetting("Multiplier").value(1.8f).range(0.5f, 6.0f).step(0.1f);

    public TargetMovementPrediction() {
        addSettings(prediction, mode, glidingOnly, multiplier);

        for (Setting<?> setting : getSettings()) {
            if (setting == prediction) continue;

            setting.setVisible(isPredict);
        }
    }

    public Vec3d predictPosition(LivingEntity target, Vec3d targetPosition) {
        if (!prediction.getValue() || MathUtil.getEntityBPS(target) < 13 || (glidingOnly.getValue() && !target.isGliding())) {
            return targetPosition;
        }

        double mult = multiplier.getValue();
        return predictMode.predict(target, targetPosition, mult);
    }

    private enum PredictMode implements ModeSetting.NamedChoice {
        SIMPLE("Simple", (target, targetPosition, multiplier) ->
                targetPosition.add(target.getVelocity().multiply(multiplier))
        ),

        VELOCITY("Velocity", (target, targetPosition, multiplier) -> {
            Vec3d simple = SIMPLE.predict(target, targetPosition, multiplier);
            return simple.subtract(0.0, 0.5 * 0.05 * multiplier * multiplier, 0.0);
        });

        private final String name;
        private final PredictFunction predict;

        @Override
        public String getName() {
            return name;
        }

        PredictMode(String name, PredictFunction predict) {
            this.name = name;
            this.predict = predict;
        }

        public Vec3d predict(LivingEntity target, Vec3d targetPosition, double multiplier) {
            return predict.apply(target, targetPosition, multiplier);
        }

        @FunctionalInterface
        interface PredictFunction {
            Vec3d apply(LivingEntity target, Vec3d targetPosition, double multiplier);
        }
    }
}

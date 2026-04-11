package sweetie.evaware.client.features.modules.render.targetesp;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.client.features.modules.combat.AuraModule;

public abstract class TargetEspMode implements QuickImports {
    public static final AnimationUtil showAnimation = new AnimationUtil();
    public static final AnimationUtil sizeAnimation = new AnimationUtil();
    public static LivingEntity currentTarget = null;
    public float prevShowAnimation = 0f;
    public float prevSizeAnimation = 0f;

    public AuraModule aura() {
        return AuraModule.getInstance();
    }

    public void updateTarget() {
        if (aura().target != null) {
            currentTarget = aura().target;
        }
    }

    public void updateAnimation(long duration, String mode, float size, float in, float out) {
        prevShowAnimation = (float) showAnimation.getValue();
        prevSizeAnimation = (float) sizeAnimation.getValue();

        sizeAnimation.update();
        double dyingSize = switch (mode) {
            case "In" -> in;
            case "Out" -> out;
            default -> size;
        };
        sizeAnimation.run(reason() ? size : dyingSize, duration, Easing.SINE_OUT);

        showAnimation.update();
        showAnimation.run(reason() ? 1.0 : 0.0, duration, Easing.SINE_OUT);
    }

    public boolean reason() {
        return aura().isEnabled() && aura().target != null;
    }

    public boolean canDraw() {
        if (mc.player == null || mc.world == null) return false;
        return showAnimation.getValue() > 0.0;
    }

    public static void updatePositions() {
        float animationValue = (float) showAnimation.getValue();
        float animationTarget = (float) showAnimation.getToValue();

        boolean useLastPosition = TargetEspModule.getInstance().lastPosition.getValue();
        boolean preventUpdate = useLastPosition && animationTarget == 0.0 && animationValue <= 0.9f;

        if (currentTarget != null && !preventUpdate) {
            lastTargetX = MathUtil.interpolate((float) currentTarget.prevX, (float) currentTarget.getX());
            lastTargetY = MathUtil.interpolate((float) currentTarget.prevY, (float) currentTarget.getY());
            lastTargetZ = MathUtil.interpolate((float) currentTarget.prevZ, (float) currentTarget.getZ());
        }

        targetX = lastTargetX;
        targetY = lastTargetY;
        targetZ = lastTargetZ;
    }

    @Getter private static double targetX = -1;
    @Getter private static double targetY = -1;
    @Getter private static double targetZ = -1;

    private static double lastTargetX = -1;
    private static double lastTargetY = -1;
    private static double lastTargetZ = -1;

    public abstract void onUpdate();
    public abstract void onRender3D(Render3DEvent.Render3DEventData event);
}

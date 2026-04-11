package sweetie.evaware.inject.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import sweetie.evaware.api.event.events.player.other.MovementInputEvent;
import sweetie.evaware.api.event.events.player.move.SprintEvent;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.player.DirectionalInput;
import sweetie.evaware.api.utils.player.MoveUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;
import sweetie.evaware.client.features.modules.combat.AuraModule;
import sweetie.evaware.client.features.modules.movement.MoveFixModule;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput onTick(PlayerInput original) {
        MovementInputEvent.MovementInputEventData movementInputEvent = new MovementInputEvent.MovementInputEventData(original, original.jump(), original.sneak(), new DirectionalInput(original));
        MovementInputEvent.getInstance().call(movementInputEvent);

        DirectionalInput untransformedDirectionalInput = movementInputEvent.getDirectionalInput();
        DirectionalInput directionalInput = transformDirection(untransformedDirectionalInput);

        SprintEvent.SprintEventData sprintEvent = new SprintEvent.SprintEventData(directionalInput);
        SprintEvent.getInstance().call(sprintEvent);

        this.untransformed = new PlayerInput(
                untransformedDirectionalInput.isForwards(),
                untransformedDirectionalInput.isBackwards(),
                untransformedDirectionalInput.isLeft(),
                untransformedDirectionalInput.isRight(),
                original.jump(),
                original.sneak(),
                sprintEvent.isSprint()
        );

        return new PlayerInput(
                directionalInput.isForwards(),
                directionalInput.isBackwards(),
                directionalInput.isLeft(),
                directionalInput.isRight(),
                movementInputEvent.isJump(),
                movementInputEvent.isSneak(),
                sprintEvent.isSprint()
        );
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput input) {
        ClientPlayerEntity player = SharedClass.player();
        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getCurrentRotation();
        RotationPlan rotationPlan = rotationManager.getCurrentRotationPlan();

        if (rotationPlan == null || rotation == null || player == null || !rotationPlan.moveCorrection()) {
            return input;
        }

        float z = KeyboardInput.getMovementMultiplier(input.isForwards(), input.isBackwards());
        float x = KeyboardInput.getMovementMultiplier(input.isLeft(), input.isRight());

        float yaw = rotation.getYaw();
        float direction = player.getYaw();

        if (MoveFixModule.isTargeting() && !(z != 1 || x != 0)) {
            AuraModule auraModule = AuraModule.getInstance();

            Vec3d position = auraModule.target != null ? auraModule.target.getPos() : null;

            if (position == null) {
                return input;
            }

            double deltaX = position.x - player.getX();
            double deltaZ = position.z - player.getZ();

            double angleToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
            angleToTarget = MathHelper.wrapDegrees(angleToTarget);

            float bestForward = 0F;
            float bestStrafe = 0F;
            float minDifference = Float.MAX_VALUE;

            for (float forward = -1F; forward <= 1F; forward += 1F) {
                for (float strafe = -1F; strafe <= 1F; strafe += 1F) {
                    if (forward == 0F && strafe == 0F) {
                        continue;
                    }

                    double moveAngle = MoveUtil.direction(yaw, forward, strafe);
                    moveAngle = Math.toDegrees(moveAngle);
                    moveAngle = MathHelper.wrapDegrees(moveAngle);

                    double difference = Math.abs(MathHelper.wrapDegrees(angleToTarget - moveAngle));
                    difference = Math.min(difference, 360 - difference);

                    if (difference < minDifference) {
                        minDifference = (float) difference;
                        bestForward = forward;
                        bestStrafe = strafe;
                    }
                }
            }
            return new DirectionalInput(bestForward, bestStrafe);
        } else if (rotationPlan.freeMoveCorrection()) {
            float deltaYaw = direction - yaw;
            float radians = deltaYaw * 0.017453292f;

            float newX = x * MathHelper.cos(radians) - z * MathHelper.sin(radians);
            float newZ = z * MathHelper.cos(radians) + x * MathHelper.sin(radians);

            int movementSideways = Math.round(newX);
            int movementForward = Math.round(newZ);

            return new DirectionalInput(movementForward, movementSideways);
        }

        return input;
    }
}

package sweetie.evaware.inject.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.rotation.RaytracingUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.client.features.modules.combat.NoEntityTraceModule;
import sweetie.evaware.client.features.modules.render.RemovalsModule;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    /**
     * raycast
     **/
    @ModifyExpressionValue(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"))
    private EntityHitResult traceEntityHook(EntityHitResult original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (original == null || original.getEntity() == null || SharedClass.player() == null) return original;

        if (NoEntityTraceModule.getInstance().shouldCancelResult(original.getEntity())) {
            return null;
        }

        return original;
    }

    @ModifyExpressionValue(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d rotationVectorHook(Vec3d original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != SharedClass.player()) {
            return original;
        }

        var rotation = RotationManager.getInstance().getCurrentRotation();
        return rotation != null ? rotation.getVector() : original;
    }

    @ModifyExpressionValue(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult hookRaycast(HitResult original, Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        if (camera != SharedClass.player()) {
            return original;
        }

        var cameraRotation = new Rotation(camera.getYaw(tickDelta), camera.getPitch(tickDelta));

        Rotation rotation;
        if (RotationManager.getInstance().getCurrentRotation() != null) {
            rotation = RotationManager.getInstance().getCurrentRotation();
        } else {
            rotation = cameraRotation;
        }

        return RaytracingUtil.raycast(rotation, Math.max(blockInteractionRange, entityInteractionRange), false, tickDelta);
    }

    /**
     * render
     */
    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    void render3D(RenderTickCounter renderTickCounter, CallbackInfo ci) {
        if (SharedClass.player() == null || MinecraftClient.getInstance().world == null) return;

        MatrixStack matrixStack = new MatrixStack();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        Render3DEvent.getInstance().call(new Render3DEvent.Render3DEventData(matrixStack, renderTickCounter.getTickDelta(false)));
        RenderUtil.BOX.setup3DRender(matrixStack);
        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        if (RemovalsModule.getInstance().isHurtCamera()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float onRenderWorld(float delta, float first, float second) {
        if (MinecraftClient.getInstance().player == null) return 0;

        if (RemovalsModule.getInstance().isBadEffects()) {
            return 0;
        }
        return MathHelper.lerp(delta, first, second);
    }
}

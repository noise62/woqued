package sweetie.evaware.inject.other;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;

@Mixin(Item.class)
public abstract class MixinItem {
    @ModifyExpressionValue(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d hookFixRotation(Vec3d original, World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling) {
        var rotation = RotationManager.getInstance().getCurrentRotation();

        if (player == SharedClass.player() && rotation != null) {
            return rotation.getVector();
        }

        return original;
    }
}

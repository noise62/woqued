package worst.woqued.inject.other;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import worst.woqued.client.features.commands.CommandSkin;
import worst.woqued.client.features.modules.render.NavalnyModule;

@Mixin(PlayerListEntry.class)
public class MixinPlayerListEntry {
    @ModifyReturnValue(method = "getSkinTextures", at = @At("RETURN"))
    private SkinTextures skinTexturesHook(SkinTextures original) {
        if (NavalnyModule.getInstance().isEnabled()) {
            return new SkinTextures(
                    NavalnyModule.getInstance().getNavalnySkin(),
                    "local",
                    null,
                    null,
                    SkinTextures.Model.WIDE,
                    false
            );
        }

        var customSkin = CommandSkin.getCustomSkinTextures();
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            if (customSkin != null) {
                var playerListEntry = player.getPlayerListEntry();
                if (playerListEntry != null && playerListEntry.equals(this)) {
                    original = customSkin.get();
                }
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = "texturesSupplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;uuidEquals(Ljava/util/UUID;)Z"))
    private static boolean texturesSupplierHook(boolean original) {
        return original || CommandSkin.getCustomSkinTextures() != null || NavalnyModule.getInstance().isEnabled();
    }
}
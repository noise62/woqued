package worst.woqued.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import worst.woqued.client.features.modules.render.NavalnyModule;
import worst.woqued.client.features.modules.render.CapeModule;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {

    @Unique
    private static final Identifier CUSTOM_CAPE = Identifier.of("evaware", "pocoy.png");

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (NavalnyModule.getInstance().isEnabled()) {
            SkinTextures original = cir.getReturnValue();

            SkinTextures navalnySkin = new SkinTextures(
                    NavalnyModule.getInstance().getNavalnySkin(),
                    "local",
                    null,
                    null,
                    SkinTextures.Model.WIDE,
                    false
            );

            SkinTextures modified = new SkinTextures(
                    navalnySkin.texture(),
                    navalnySkin.textureUrl(),
                    original.capeTexture(),
                    original.elytraTexture(),
                    navalnySkin.model(),
                    navalnySkin.secure()
            );
            cir.setReturnValue(modified);
            return;
        }

        AbstractClientPlayerEntity entity = (AbstractClientPlayerEntity) (Object) this;
        if (entity == MinecraftClient.getInstance().player && CapeModule.getInstance().isEnabled()) {
            SkinTextures original = cir.getReturnValue();

            SkinTextures modified = new SkinTextures(
                    original.texture(),
                    original.textureUrl(),
                    CUSTOM_CAPE,
                    CUSTOM_CAPE,
                    original.model(),
                    original.secure()
            );

            cir.setReturnValue(modified);
        }
    }
}
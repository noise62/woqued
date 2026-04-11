package sweetie.evaware.inject.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweetie.evaware.client.features.modules.render.CapeModule;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {

    // Указываем путь к твоей текстуре (assets/evaware/pocoy.png)
    @Unique
    private static final Identifier CUSTOM_CAPE = Identifier.of("evaware", "pocoy.png");

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        // Получаем объект текущего игрока, для которого запрашивается текстура
        AbstractClientPlayerEntity entity = (AbstractClientPlayerEntity) (Object) this;

        // Проверяем: 1. Это ТЫ (локальный игрок)? 2. Включен ли модуль?
        if (entity == MinecraftClient.getInstance().player && CapeModule.getInstance().isEnabled()) {

            SkinTextures original = cir.getReturnValue();

            // Record-классы нельзя изменять (они immutable), поэтому создаем копию,
            // заменяя только плащ (capeTexture) и элитры (elytraTexture) на наш кастомный.
            SkinTextures modified = new SkinTextures(
                    original.texture(),     // Скин остается оригинальным
                    original.textureUrl(),  // URL скина
                    CUSTOM_CAPE,            // НАШ ПЛАЩ
                    CUSTOM_CAPE,            // НАШИ ЭЛИТРЫ (обычно используют ту же текстуру)
                    original.model(),       // Моделька (Alex/Steve)
                    original.secure()       // Безопасность подписи
            );

            // Возвращаем подмененные текстуры
            cir.setReturnValue(modified);
        }
    }
}
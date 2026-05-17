package worst.woqued.inject.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import worst.woqued.client.features.modules.render.AmbienceModule;

@Mixin(ClientWorld.Properties.class)
public class MixinClientWorldProperties {
    @ModifyReturnValue(method = "getTimeOfDay", at = @At("RETURN"))
    private long getTimeOfDay(long original) {
        return AmbienceModule.getInstance().getTime(original);
    }
}
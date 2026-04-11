package sweetie.evaware.inject.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.client.features.modules.render.AmbienceModule;

@Mixin(World.class)
public class MixinWorld {
    @Inject(method = "getRainGradient", cancellable = true, at = @At("HEAD"))
    private void overrideWeather(float delta, CallbackInfoReturnable<Float> cir) {
        AmbienceModule module = AmbienceModule.getInstance();
        AmbienceModule.Weather weather = Choice.getChoiceByName(module.weather.getValue(), AmbienceModule.Weather.values());
        if (module.isEnabled()) {
            switch (weather) {
                case SUNNY -> cir.setReturnValue(0.0f);
                case RAINY, THUNDER -> cir.setReturnValue(1.0f);
                case SNOWY -> cir.setReturnValue(0.9f);
            }
        }
    }

    @Inject(method = "getThunderGradient", cancellable = true, at = @At("HEAD"))
    private void overrideThunder(float delta, CallbackInfoReturnable<Float> cir) {
        AmbienceModule module = AmbienceModule.getInstance();
        AmbienceModule.Weather weather = Choice.getChoiceByName(module.weather.getValue(), AmbienceModule.Weather.values());
        if (module.isEnabled()) {
            switch (weather) {
                case SUNNY, RAINY, SNOWY -> cir.setReturnValue(0.0f);
                case THUNDER -> cir.setReturnValue(1.0f);
            }
        }
    }

}
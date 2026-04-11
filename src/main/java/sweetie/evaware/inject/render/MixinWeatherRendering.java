package sweetie.evaware.inject.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sweetie.evaware.client.features.modules.render.AmbienceModule;

@Mixin(WeatherRendering.class)
public abstract class MixinWeatherRendering {

    @ModifyExpressionValue(method = "addParticlesAndSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getRainGradient(F)F"))
    private float ambientPrecipitation(float original) {
        var moduleCustomAmbience = AmbienceModule.getInstance();
        if (moduleCustomAmbience.isEnabled() && moduleCustomAmbience.weather.is(AmbienceModule.Weather.SNOWY)) {
            return 0f;
        }

        return original;
    }

    @ModifyReturnValue(method = "getPrecipitationAt", at = @At(value = "RETURN", ordinal = 1))
    private Biome.Precipitation modifyBiomePrecipitation(Biome.Precipitation original) {
        var moduleOverrideWeather = AmbienceModule.getInstance();
        if (moduleOverrideWeather.isEnabled() && moduleOverrideWeather.weather.is(AmbienceModule.Weather.SNOWY)) {
            return Biome.Precipitation.SNOW;
        }

        return original;
    }

}
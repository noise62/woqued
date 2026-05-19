package worst.woqued.client.features.modules.render.chinahat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

public final class ChinaHatLighting {
    private static final float COLD_R = 167f / 255f;
    private static final float COLD_G = 180f / 255f;
    private static final float COLD_B = 1f;
    private static final float WARM_R = 1f;
    private static final float WARM_G = 191f / 255f;
    private static final float WARM_B = 117f / 255f;

    public HatTint resolve(PlayerEntity player, Vec3d hatWorldPos, float alpha) {
        if (player == null || player.getWorld() == null) {
            return new HatTint(1f, 1f, 1f, alpha);
        }

        BlockPos lightPos = BlockPos.ofFloored(hatWorldPos);
        float biomeTemperature = player.getWorld().getBiome(lightPos).value().getTemperature();
        float temperatureMix = MathHelper.clamp(biomeTemperature / 2f, 0f, 1f);

        float tintR = MathHelper.lerp(temperatureMix, COLD_R, WARM_R);
        float tintG = MathHelper.lerp(temperatureMix, COLD_G, WARM_G);
        float tintB = MathHelper.lerp(temperatureMix, COLD_B, WARM_B);

        int skyLight = player.getWorld().getLightLevel(LightType.SKY, lightPos);
        int blockLight = player.getWorld().getLightLevel(LightType.BLOCK, lightPos);
        float combinedBrightness = MathHelper.clamp((skyLight * 0.75f + blockLight * 0.55f) / 15f, 0f, 1f);
        float ambientBrightness = MathHelper.clamp(1f - player.getWorld().getAmbientDarkness() / 15f, 0.15f, 1f);
        float brightness = MathHelper.clamp(combinedBrightness * 0.7f + ambientBrightness * 0.3f, 0.18f, 1f);

        return new HatTint(tintR * brightness, tintG * brightness, tintB * brightness, alpha);
    }

    public record HatTint(float red, float green, float blue, float alpha) {
    }
}

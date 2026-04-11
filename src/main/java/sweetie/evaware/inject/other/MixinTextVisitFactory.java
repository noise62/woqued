package sweetie.evaware.inject.other;

import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.other.ReplaceUtil;
import sweetie.evaware.client.features.modules.other.StreamerModule;

@Mixin(TextVisitFactory.class)
public class MixinTextVisitFactory {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", ordinal = 0), method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", index = 0)
    private static String visitFormatted(String string) {
        if (!StreamerModule.getInstance().isEnabled() || SharedClass.player() == null) {
            return string;
        }

        return ReplaceUtil.protectedString(string);
    }
}

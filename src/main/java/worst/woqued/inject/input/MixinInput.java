package worst.woqued.inject.input;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import worst.woqued.api.system.interfaces.IPlayerInput;

@Mixin(Input.class)
public abstract class MixinInput implements IPlayerInput {
    @Unique
    protected PlayerInput untransformed = PlayerInput.DEFAULT;

    @Override
    public PlayerInput evelina$getUntransformed() {
        return untransformed;
    }
}

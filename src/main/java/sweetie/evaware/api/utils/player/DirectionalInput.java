package sweetie.evaware.api.utils.player;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import sweetie.evaware.api.system.interfaces.IPlayerInput;

@Getter
@Setter
public final class DirectionalInput {
    private boolean forwards;
    private boolean backwards;
    private boolean left;
    private boolean right;

    public static final DirectionalInput NONE = new DirectionalInput(false, false, false, false);
    public static final DirectionalInput FORWARDS = new DirectionalInput(true, false, false, false);
    public static final DirectionalInput BACKWARDS = new DirectionalInput(false, true, false, false);
    public static final DirectionalInput LEFT = new DirectionalInput(false, false, true, false);
    public static final DirectionalInput RIGHT = new DirectionalInput(false, false, false, true);

    public DirectionalInput(boolean forwards, boolean backwards, boolean left, boolean right) {
        this.forwards = forwards;
        this.backwards = backwards;
        this.left = left;
        this.right = right;
    }

    public DirectionalInput(Input input) {
        this(((IPlayerInput) input).evelina$getUntransformed());
    }

    public DirectionalInput(PlayerInput input) {
        this(input.forward(), input.backward(), input.left(), input.right());
    }

    public DirectionalInput(float movementForward, float movementSideways) {
        this(
                movementForward > 0.0f,
                movementForward < 0.0f,
                movementSideways > 0.0f,
                movementSideways < 0.0f
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DirectionalInput that)) return false;
        return forwards == that.forwards &&
                backwards == that.backwards &&
                left == that.left &&
                right == that.right;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(forwards);
        result = 31 * result + Boolean.hashCode(backwards);
        result = 31 * result + Boolean.hashCode(left);
        result = 31 * result + Boolean.hashCode(right);
        return result;
    }

    public boolean isMoving() {
        return forwards || backwards || left || right;
    }
}

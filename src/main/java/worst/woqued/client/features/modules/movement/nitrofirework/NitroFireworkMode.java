package worst.woqued.client.features.modules.movement.nitrofirework;

import worst.woqued.api.system.backend.Choice;
import worst.woqued.api.system.backend.Pair;

public abstract class NitroFireworkMode extends Choice {
    public abstract Pair<Float, Float> velocityValues();
}

package sweetie.evaware.client.features.modules.movement.nitrofirework;

import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.api.system.backend.Pair;
import sweetie.evaware.api.system.interfaces.QuickImports;

public abstract class NitroFireworkMode extends Choice {
    public abstract Pair<Float, Float> velocityValues();
}

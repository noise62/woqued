package sweetie.evaware.client.features.modules.movement.noslow;

import sweetie.evaware.api.system.backend.Choice;

public abstract class NoSlowMode extends Choice {
    public abstract void onUpdate();
    public abstract void onTick();
    public abstract boolean slowingCancel();
}

package worst.woqued.client.features.modules.movement.speed;

import worst.woqued.api.system.backend.Choice;

public abstract class SpeedMode extends Choice {


    // events
    public void onUpdate() {}
    public void onTravel() {}

    // module methods
    public void onEnable() {}
    public void onDisable() {}
    public void toggle() {}
}

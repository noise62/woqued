package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.configs.FriendManager;

@ModuleRegister(name = "Auto Leave", category = Category.PLAYER)
public class AutoLeaveModule extends Module {
    @Getter private static final AutoLeaveModule instance = new AutoLeaveModule();

    private final SliderSetting distance = new SliderSetting("Distance").value(50f).range(1f, 100f).step(1f);
    private final ModeSetting action = new ModeSetting("Action").value("Spawn").values("Hub", "Spawn", "Home");

    public AutoLeaveModule() {
        addSettings(distance, action);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handleUpdateEvent();
        }));

        addEvents(updateEvent);
    }

    private void handleUpdateEvent() {
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (mc.player == player) continue;
            if (FriendManager.getInstance().contains(player.getName().getString())) continue;

            if (player.getPos().distanceTo(mc.player.getPos()) <= distance.getValue()) {
                handleLeave();
                toggle();
                break;
            }
        }
    }

    private void handleLeave() {
        switch (action.getValue()) {
            case "Hub" -> mc.player.networkHandler.sendChatCommand("hub");
            case "Spawn" -> mc.player.networkHandler.sendChatCommand("spawn");
            case "Home" -> mc.player.networkHandler.sendChatCommand("home home");
        }
    }
}

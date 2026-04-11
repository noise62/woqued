package sweetie.evaware.client.features.modules.movement.noslow.modes;

import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import sweetie.evaware.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowGrim extends NoSlowMode {
    @Override
    public String getName() {
        return "Grim";
    }
    private int ticks = 0;

    public BypassType bypassType = BypassType.TICK;

    @Override
    public void onUpdate() {
        switch (bypassType) {
            case OLD -> {
                if (slowingCancel() && mc.player.isUsingItem()) {
                    Hand hand = mc.player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    sendPacket(new PlayerInteractItemC2SPacket(hand, 0, mc.player.getYaw(), mc.player.getPitch()));
                }
            }

            case TICK -> {

            }
        }
    }

    @Override
    public void onTick() {
        if (mc.player.isUsingItem()) {
            ticks++;
        } else {
            ticks = 0;
        }
    }

    @Override
    public boolean slowingCancel() {
        boolean tickRule = ticks >= 2;
        if (tickRule) ticks = 0;

        boolean cancelRule = false;

        switch (bypassType) {
            case TICK -> {
                cancelRule = tickRule;
            }

            case OLD -> {
                cancelRule = true;
            }
        }

        return cancelRule;
    }

    public enum BypassType {
        TICK, OLD
    }
}

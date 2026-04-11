package sweetie.evaware.api.event.events.player.world;

import lombok.Getter;
import net.minecraft.screen.slot.SlotActionType;
import sweetie.evaware.api.event.events.Event;

public class ClickSlotEvent extends Event<ClickSlotEvent.ClickSlotEventData> {
    @Getter private static final ClickSlotEvent instance = new ClickSlotEvent();

    public record ClickSlotEventData(SlotActionType slotActionType, int slot, int button, int id) {
    }
}

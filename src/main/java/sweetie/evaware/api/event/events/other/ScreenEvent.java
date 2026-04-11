package sweetie.evaware.api.event.events.other;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import sweetie.evaware.api.event.events.Event;

import java.util.ArrayList;
import java.util.List;

public class ScreenEvent extends Event<ScreenEvent.ScreenEventData> {
    @Getter private static final ScreenEvent instance = new ScreenEvent();

    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    public static class ScreenEventData {
        private final Screen screen;
        private final List<ButtonWidget> buttons = new ArrayList<>();
    }
}

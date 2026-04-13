package sweetie.evaware.client.ui.widget;

import lombok.Getter;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.client.features.modules.render.InterfaceModule;
import sweetie.evaware.client.ui.widget.overlay.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WidgetManager {
    @Getter private final static WidgetManager instance = new WidgetManager();

    private final List<Widget> widgets = new ArrayList<>();

    public void load() {
        register(
                new WatermarkWidget(),
                new KeybindsWidget(),
                new PotionsWidget(),
                new StaffsWidget(),
                new CooldownsWidget(),
                new ArrayListWidget(),

                new ArmorWidget(),

                new TargetInfoWidget(),

                new BPSWidget(),
                new XYZWidget()
        );

        InterfaceModule.getInstance().init();

        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (InterfaceModule.getInstance().isEnabled()) {
                for (Widget widget : widgets) {
                    if (widget.isEnabled()) widget.render(event);
                }
            }
        }));
    }

    public void register(Widget... widgets) {
        this.widgets.addAll(List.of(widgets));
    }
}

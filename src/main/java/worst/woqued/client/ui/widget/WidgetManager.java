package worst.woqued.client.ui.widget;

import lombok.Getter;
import net.minecraft.block.MushroomBlock;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.client.features.modules.render.InterfaceModule;
import worst.woqued.client.ui.widget.overlay.*;
import worst.woqued.client.ui.widget.overlay.*;
import worst.woqued.client.ui.widget.overlay.MusicWidget;

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
                new BossBarWidget(),
                new ScoreboardWidget(),
                new ArrayListWidget(),

                new ArmorWidget(),

                new TargetInfoWidget(),

                new BPSWidget(),
                new XYZWidget(),
                new MusicWidget()
        );

        InterfaceModule.getInstance().init();

        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (InterfaceModule.getInstance().isEnabled()) {
                for (Widget widget : widgets) {
                    if (widget.isEnabled()) widget.render(event);
                }
            }
        }));

        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            for (Widget widget : widgets) {
                if (widget instanceof MusicWidget musicWidget) {
                    musicWidget.tick();
                }
            }
        }));
    }

    public void register(Widget... widgets) {
        this.widgets.addAll(List.of(widgets));
    }
}

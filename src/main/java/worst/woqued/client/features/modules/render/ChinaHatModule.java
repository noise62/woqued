package worst.woqued.client.features.modules.render;

import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.client.features.modules.render.chinahat.ChinaHatController;
import worst.woqued.client.features.modules.render.chinahat.ChinaHatRenderer;
import worst.woqued.client.features.modules.render.chinahat.ChinaHatSettings;
import lombok.Getter;

@ModuleRegister(name = "China Hat", category = Category.RENDER)
public class ChinaHatModule extends Module {
    @Getter
    private static final ChinaHatModule instance = new ChinaHatModule();

    private final ChinaHatSettings settings = new ChinaHatSettings();
    private final ChinaHatController controller = new ChinaHatController(this, settings);
    private final ChinaHatRenderer renderer = new ChinaHatRenderer();

    public ChinaHatModule() {
        addSettings(settings.asSettings());
    }

    @Override
    public void onDisable() {
        controller.clear();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event ->
                controller.updateTargets()));
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event ->
                renderer.render(event, settings, controller.getTargets())));

        addEvents(updateEvent, renderEvent);
    }
}

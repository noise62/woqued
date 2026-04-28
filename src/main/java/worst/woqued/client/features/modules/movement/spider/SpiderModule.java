package worst.woqued.client.features.modules.movement.spider;

import lombok.Getter;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.move.MotionEvent;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.system.backend.Choice;
import worst.woqued.client.features.modules.movement.spider.modes.SpiderFunTime;
import worst.woqued.client.features.modules.movement.spider.modes.SpiderMatrix;

@ModuleRegister(name = "Spider", category = Category.MOVEMENT)
public class SpiderModule extends Module {
    @Getter private static final SpiderModule instance = new SpiderModule();

    private final SpiderFunTime spiderFunTime = new SpiderFunTime();
    private final SpiderMatrix spiderMatrix = new SpiderMatrix(() -> getMode().is("Matrix"));

    private final SpiderMode[] modes = new SpiderMode[]{
            spiderFunTime, spiderMatrix
    };

    private SpiderMode currentMode = spiderFunTime;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(spiderFunTime.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                currentMode = (SpiderMode) Choice.getChoiceByName(getMode().getValue(), modes);
            });

    public SpiderModule() {
        addSettings(mode);

        for (SpiderMode spiderMode : modes) {
            addSettings(spiderMode.getSettings());
        }
    }

    @Override
    public void onEvent() {
        EventListener motionEvent = MotionEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onMotion(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        addEvents(motionEvent, updateEvent);
    }
}

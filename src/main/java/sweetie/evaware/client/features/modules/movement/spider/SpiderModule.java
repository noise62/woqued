package sweetie.evaware.client.features.modules.movement.spider;

import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.move.MotionEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.system.backend.Choice;
import sweetie.evaware.client.features.modules.movement.spider.modes.SpiderFunTime;
import sweetie.evaware.client.features.modules.movement.spider.modes.SpiderMatrix;

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

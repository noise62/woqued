package sweetie.evaware.client.features.modules.render.targetesp;

import lombok.Getter;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.client.features.modules.render.targetesp.modes.TargetEspComets;
import sweetie.evaware.client.features.modules.render.targetesp.modes.TargetEspTexture;

@ModuleRegister(name = "Target Esp", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();

    private final TargetEspComets espComets = new TargetEspComets();
    private final TargetEspTexture espTexture = new TargetEspTexture();

    private TargetEspMode currentMode = espTexture;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Marker").values(
            "Marker", "Comets"
    ).onAction(() -> {
        currentMode = switch (getMode().getValue()) {
            case "Comets" -> espComets;
            default -> espTexture;
        };
    });
    private final ModeSetting animation = new ModeSetting("Animation").value("In").values("In", "Out", "None");
    private final SliderSetting duration = new SliderSetting("Duration").value(3f).range(1f, 20f).step(1f);
    private final SliderSetting size = new SliderSetting("Size").value(1f).range(0.1f, 2f).step(0.1f);
    private final SliderSetting inSize = new SliderSetting("In size").value(0f).range(0f, 1f).step(0.1f).setVisible(() -> animation.is("In"));
    private final SliderSetting outSize = new SliderSetting("Out size").value(2f).range(1f, 2f).step(0.1f).setVisible(() -> animation.is("Out"));
    public final BooleanSetting lastPosition = new BooleanSetting("Last position").value(true);

    public TargetEspModule() {
        addSettings(mode, animation, duration, size, inSize, outSize, lastPosition);
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            TargetEspMode.updatePositions();

            currentMode.onRender3D(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.updateAnimation(duration.getValue().longValue() * 50, animation.getValue(), size.getValue(), inSize.getValue(), outSize.getValue());
            currentMode.updateTarget();
            currentMode.onUpdate();
        }));

        addEvents(render3DEvent, updateEvent);
    }
}

package worst.woqued.client.features.modules.render.targetesp;

import lombok.Getter;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.client.features.modules.render.targetesp.modes.TargetEspComets;
import worst.woqued.client.features.modules.render.targetesp.modes.TargetEspCrystals;
import worst.woqued.client.features.modules.render.targetesp.modes.TargetEspTexture;
import worst.woqued.client.features.modules.render.targetesp.modes.TargetEspChains;

@ModuleRegister(name = "Target Esp", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();

    private final TargetEspComets espComets = new TargetEspComets();
    private final TargetEspTexture espTexture = new TargetEspTexture();
    private final TargetEspCrystals espCrystals = new TargetEspCrystals();
    private final TargetEspChains espChains = new TargetEspChains();

    private TargetEspMode currentMode = espTexture;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Marker").values(
            "Marker", "Comets", "Crystals", "Chains"
    ).onAction(() -> {
        currentMode = switch (getMode().getValue()) {
            case "Comets" -> espComets;
            case "Crystals" -> espCrystals;
            case "Chains" -> espChains;
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
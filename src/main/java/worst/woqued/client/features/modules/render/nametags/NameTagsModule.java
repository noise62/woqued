package worst.woqued.client.features.modules.render.nametags;

import lombok.Getter;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ColorSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.module.setting.MultiBooleanSetting;
import worst.woqued.api.utils.combat.TargetManager;

import java.awt.*;
import java.util.function.Supplier;

@ModuleRegister(name = "Name Tags", category = Category.RENDER)
public class NameTagsModule extends Module {
    @Getter private static final NameTagsModule instance = new NameTagsModule();

    public final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Self").value(false),
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Animals").value(false),
            new BooleanSetting("Mobs").value(false)
    );
    public final SliderSetting scale = new SliderSetting("Scale").value(1f).range(0.1f, 2f).step(0.1f);
    public final MultiBooleanSetting information = new MultiBooleanSetting("Information").value(
            new BooleanSetting("Items").value(true),
            new BooleanSetting("Potions").value(true)
    );

    private final Supplier<Boolean> itemsIsEnabled = () -> information.isEnabled("Items");

    public final MultiBooleanSetting options = new MultiBooleanSetting("Options").value(
            new BooleanSetting("Special items").value(false).setVisible(itemsIsEnabled),
            new BooleanSetting("Enchants").value(true).setVisible(itemsIsEnabled),
            new BooleanSetting("Only hands").value(false).setVisible(itemsIsEnabled)
    );

    public final SliderSetting glassy = new SliderSetting("Glassy").value(0.5f).range(0.0f, 1f).step(0.1f);
    public final BooleanSetting box3d = new BooleanSetting("3D Box").value(false);
    public final SliderSetting boxAlpha = new SliderSetting("Box Alpha").value(0.3f).range(0.0f, 1f).step(0.1f).setVisible(() -> box3d.getValue());
    public final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final NameTagsRender nameTagsRender = new NameTagsRender(this);

    public NameTagsModule() {
        addSettings(targets, scale, information, options, glassy, box3d, boxAlpha);
    }

    @Override
    public void onEvent() {
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(1, event -> {
            entityFilter.targetSettings = targets.getList();
            entityFilter.needFriends = true;

            nameTagsRender.onRender2D(event);
        }));

        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            entityFilter.targetSettings = targets.getList();
            entityFilter.needFriends = true;

            nameTagsRender.onRender3D(event);
        }));

        addEvents(render2DEvent, render3DEvent);
    }
}

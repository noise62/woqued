package sweetie.evaware.client.features.modules.render.nametags;

import lombok.Getter;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ColorSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.utils.combat.TargetManager;

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
    public final ColorSetting textColor = new ColorSetting("Text color").value(new Color(255, 255, 255));
    public final ColorSetting color = new ColorSetting("Color").value(new Color(20, 20, 20));
    public final ColorSetting friendColor = new ColorSetting("Friend color").value(new Color(132, 229, 121)).setVisible(() -> targets.isEnabled("Players") || targets.isEnabled("Self"));

    public final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final NameTagsRender nameTagsRender = new NameTagsRender(this);

    public NameTagsModule() {
        addSettings(targets, scale, information, options, glassy, textColor, color, friendColor);
    }

    @Override
    public void onEvent() {
        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(1, event -> {
            entityFilter.targetSettings = targets.getList();
            entityFilter.needFriends = true;

            nameTagsRender.onRender(event);
        }));

        addEvents(render2DEvent);
    }
}

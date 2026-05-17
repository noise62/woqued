package worst.woqued.client.features.modules.render;

import lombok.Getter;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.module.setting.MultiBooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.RunSetting;
import worst.woqued.api.utils.render.KawaseBlurProgram;
import worst.woqued.client.services.RenderService;
import worst.woqued.client.ui.theme.ThemeEditor;
import worst.woqued.client.ui.widget.WidgetManager;
import worst.woqued.client.ui.widget.overlay.TargetInfoWidget;

@ModuleRegister(name = "Interface", category = Category.RENDER)
public class InterfaceModule extends Module {
    @Getter private static final InterfaceModule instance = new InterfaceModule();

    public final MultiBooleanSetting widgets = new MultiBooleanSetting("Widgets");
    private ModeSetting targetInfoMode;
    private TargetInfoWidget targetInfoWidget;

    private ModeSetting createTargetInfoMode() {
        targetInfoWidget = (TargetInfoWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(w -> w instanceof TargetInfoWidget)
                .findFirst().orElse(null);

        return new ModeSetting("Target info mode").values(TargetInfoWidget.RenderMode.values())
                .value(TargetInfoWidget.RenderMode.BASIC)
                .setVisible(() -> {
                    if (widgets.getValue().isEmpty()) return false;
                    BooleanSetting targetInfoSetting = widgets.getValue().stream()
                            .filter(s -> s.getName().equals("Target info"))
                            .findFirst().orElse(null);
                    return targetInfoSetting != null && targetInfoSetting.getValue();
                })
                .onAction(() -> {
                    if (targetInfoWidget != null) {
                        for (TargetInfoWidget.RenderMode mode : TargetInfoWidget.RenderMode.values()) {
                            if (targetInfoMode.is(mode)) {
                                targetInfoWidget.setRenderMode(mode);
                                break;
                            }
                        }
                    }
                });
    }
    private final RunSetting themes = new RunSetting("Theme editor").value(() -> {
        ThemeEditor.getInstance().setOpen(!ThemeEditor.getInstance().isOpen());
    });
    public final SliderSetting scale = new SliderSetting("Scale").value(0.9f).range(0.6f, 1.5f).step(0.05f).onAction(() -> RenderService.getInstance().updateScale());
    public final SliderSetting glassy = new SliderSetting("Glassy").value(0.0f).range(0.0f, 1f).step(0.1f);
    public final SliderSetting passes = new SliderSetting("Passes").value(3f).range(1f, 5f).step(1f).onAction(KawaseBlurProgram::recreate);
    public final SliderSetting offset = new SliderSetting("Offset").value(12f).range(5f, 25f).step(1f);

    public static float getScale() { return getInstance().scale.getValue(); }
    public static float getGlassy() { return 1f - getInstance().glassy.getValue(); }
    public static int getPasses() { return getInstance().passes.getValue().intValue(); }
    public static float getOffset() { return getInstance().offset.getValue(); }

    public void init() {
        widgets.value(WidgetManager.getInstance().getWidgets().stream()
                .map(widget -> {
                    BooleanSetting setting = new BooleanSetting(widget.getName()).value(widget.isEnabled());
                    setting.onAction(() -> widget.setEnabled(setting.getValue()));
                    return setting;
                })
                .toList());

        targetInfoMode = createTargetInfoMode();

        addSettings(widgets, targetInfoMode, themes,
                scale, glassy, passes, offset);
    }

    @Override
    public void onEvent() {

    }
}

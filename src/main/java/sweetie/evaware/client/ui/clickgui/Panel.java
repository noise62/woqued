package sweetie.evaware.client.ui.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.other.WindowResizeEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.UIComponent;
import sweetie.evaware.client.ui.clickgui.module.ModuleComponent;
import sweetie.evaware.client.ui.clickgui.module.SettingComponent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Panel extends UIComponent {
    private final Category category;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();

    @Setter private int categoryIndex;

    private double scroll = 0f;
    private final AnimationUtil scrollAnimation = new AnimationUtil();

    public Panel(Category category) {
        this.category = category;

        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.getCategory() == category) {
                ModuleComponent moduleComponent = new ModuleComponent(module);
                moduleComponent.setRound(getRound() * 2f);
                moduleComponents.add(moduleComponent);
            }
        }

        if (!moduleComponents.isEmpty()) {
            moduleComponents.getLast().setLast(true);
        }

        int index = categoryIndex;
        for (ModuleComponent module : moduleComponents) {
            module.setIndex(index);
            index += 45;
        }

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateThings();
        renderThings(context, mouseX, mouseY, delta);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        moduleComponents.forEach(moduleComponents -> {
            moduleComponents.keyPressed(keyCode, scanCode, modifiers);
        });
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (inPanel(mouseX, mouseY)) {
            for (ModuleComponent module : moduleComponents) {
                if (!MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) continue;

                module.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (inPanel(mouseX, mouseY)) {
            for (ModuleComponent module : moduleComponents) {
                if (!MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) continue;

                module.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            scroll += verticalAmount * 20.0;
        }

    }

    private void updateThings() {
        scrollAnimation.update();
        scrollAnimation.run(scroll, 600, Easing.EXPO_OUT);

        float w = 99f;
        float h = 240f;
        setWidth(scaled(w));
        setHeight(scaled(h));
        moduleComponents.forEach(m -> m.setRound(getRound() * 2f));
    }

    private void renderThings(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        float fontSize = getHeaderHeight() * 0.475f;
        float moduleY = 0f;

        int fullAlpha = (int) (getAlpha() * 255f);

        calcModules(moduleY);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeaderHeight(), new Vector4f(getRound(), getRound(), 0f, 0f), UIColors.blur(fullAlpha));

        Fonts.PS_BOLD.drawCenteredText(matrixStack, category.getLabel(), getX() + getWidth() / 2f, getY() + getHeaderHeight() / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));

        for (ModuleComponent module : moduleComponents) {
            module.setAlpha(getAlpha());
            module.render(context, mouseX, mouseY, delta);
        }
    }

    private void calcModules(float moduleY) {
        for (ModuleComponent module : moduleComponents) {
            float openAnim = module.getAnim();

            if (openAnim > 0f) {
                float settingOffset = 0.0f;

                for (SettingComponent setting : module.getSettings()) {
                    float visibleAnim = (float) setting.getVisibleAnimation().getValue();

                    if (visibleAnim > 0.0) {
                        settingOffset += (setting.getHeight() + gap()) * visibleAnim;
                    }
                }
                settingOffset *= openAnim;
                module.setHeight(module.getDefaultHeight() + (settingOffset + gap()) * openAnim);
            } else {
                module.setHeight(module.getDefaultHeight());
            }
            module.setWidth(getWidth());
            module.setRound(getRound() / 2f);
            module.setX(getX());
            module.setY((float) (getY() + scrollAnimation.getValue() + moduleY + getHeaderHeight()));

            moduleY += module.getHeight();
        }

        setHeight(getHeaderHeight() + moduleY);
    }

    public float getHeaderHeight() {
        return scaled(18f);
    }

    public float getRound() {
        return getHeaderHeight() / 2.2f;
    }

    public boolean inPanel(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY() + getHeaderHeight(), getWidth(), getHeight() - getHeaderHeight());
    }
}

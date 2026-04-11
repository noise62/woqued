package sweetie.evaware.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.ScissorUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.clickgui.module.ExpandableComponent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MultiBooleanComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final MultiBooleanSetting setting;

    private final AnimationUtil settingsAnimation = new AnimationUtil();

    private final List<BooleanComponent> booleans = new ArrayList<>();

    public MultiBooleanComponent(MultiBooleanSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());

        for (BooleanSetting value : setting.getValue()) {
            booleans.add(new BooleanComponent(value, true));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();
        settingsAnimation.update();

        if (isOpen()) {
            getAnim().run(1.0, 300, Easing.EXPO_OUT);
            settingsAnimation.run(getValue() >= 0.9 ? 1.0 : 0.0, 300, Easing.EXPO_OUT);
        } else {
            settingsAnimation.run(0.0, 200, Easing.EXPO_OUT);
            if (settingsAnimation.getValue() <= 0.1) {
                getAnim().run(0.0, 300, Easing.EXPO_OUT);
            }
        }

        float openAnim = getValue();
        float settingsAnim = (float) settingsAnimation.getValue();

        float fontSize = getDefaultHeight() * scaled(0.45f);
        float scd = scaled(getDefaultHeight());
        int fullAlpha = (int) (getAlpha() * 255f);

        String dermo = "...";
        float dermoWidth = Fonts.PS_MEDIUM.getWidth(dermo, fontSize);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), getWidth() * 0.04f, UIColors.backgroundBlur(fullAlpha));
        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(), getX() + offset(), getY() + scd / 2f - fontSize / 2f, getWidth() - offset() * 3f - dermoWidth, fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));
        Fonts.PS_MEDIUM.drawText(matrixStack, dermo, getX() + getWidth() - offset() * 2f - dermoWidth, getY() + scd / 2f - fontSize / 2f - scaled(2f), fontSize, UIColors.inactiveTextColor(fullAlpha));

        if (openAnim > 0.0) {
            float bY = -scaled(2f) * (1f - settingsAnim);

            for (BooleanComponent component : booleans) {
                AnimationUtil anim = component.getVisibleAnimation();
                anim.update();
                anim.run(component.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
                component.setX(getX() + offset());
                component.setY(getY() + scd + bY);
                component.setWidth(getWidth() - offset() * 2f);
                bY += (float) (component.getHeight() * anim.getValue());
            }

            setHeight(scaled(getDefaultHeight()) + (bY + gap()) * openAnim);

            if (settingsAnim > 0.0) {
                RenderUtil.OTHER.scaleStart(matrixStack, getX() + getWidth() / 2f, getY() + getDefaultHeight() + getHeight() / 2f - bY, 0.95f + (0.05f * settingsAnim));
                ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getHeight());
                for (BooleanComponent component : booleans) {
                    component.setAlpha((float) (component.getVisibleAnimation().getValue() * getAlpha() * settingsAnim));
                    component.render(context, mouseX, mouseY, delta);
                }
                ScissorUtil.stop(matrixStack);
                RenderUtil.OTHER.scaleStop(matrixStack);
            }
        } else {
            updateHeight(getDefaultHeight());
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;

        for (BooleanComponent aBoolean : booleans) {
            if (aBoolean.getVisibleAnimation().getValue() < 0.8) continue;
            aBoolean.mouseClicked(mouseX, mouseY, button);
        }
    }

    private float getDefaultHeight() {
        return 16f;
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}

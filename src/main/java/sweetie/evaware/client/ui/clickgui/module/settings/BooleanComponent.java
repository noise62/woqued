package sweetie.evaware.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.clickgui.module.SettingComponent;

import java.awt.*;
import java.time.Duration;

public class BooleanComponent extends SettingComponent {
    private final BooleanSetting setting;

    private final AnimationUtil toggleAnimation = new AnimationUtil();
    private final boolean inMenu;
    private Color color;

    public BooleanComponent(BooleanSetting setting) {
        this(setting, false);
    }

    public BooleanComponent(BooleanSetting setting, boolean inMenu) {
        super(setting);
        this.setting = setting;
        updateHeight(15f);
        toggleAnimation.setValue(setting.getValue() ? 1.0 : 0.0);
        this.inMenu = inMenu;
        this.color = inMenu ? UIColors.widgetBlur() : UIColors.backgroundBlur();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(15f);

        this.color = inMenu ? UIColors.widgetBlur() : UIColors.backgroundBlur();

        toggleAnimation.update();
        toggleAnimation.run(setting.getValue() ? 1.0 : 0.0, 100, Easing.SINE_OUT);

        MatrixStack matrixStack = context.getMatrices();
        float fontSize = getHeight() * 0.45f;
        int fullAlpha = (int) (getAlpha() * 255f);


        float anim = (float) toggleAnimation.getValue();

        float checkHeight = getHeight() * 0.67f;
        float checkWidth = checkHeight * 1.9f;
        float checkX = getX() + getWidth() - checkWidth;
        float checkY = getY() + getHeight() / 2f - checkHeight / 2f;
        float checkRound = checkHeight * 0.4f;

        float baseKnob = checkHeight * 0.8f;
        float knobGap = (checkHeight - baseKnob) * 0.8f;
        float knobSize = baseKnob - knobGap;
        float knobPenis = knobGap * 1.3f;
        float knobY = getY() + getHeight() / 2f - knobSize / 2f;
        float knobX = checkX + knobPenis + ((checkWidth - knobSize - knobPenis * 2f) * anim);
        float knobRound = knobSize * 0.4f;

        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(), getX(), getY() + getHeight() / 2f - fontSize / 2f, getWidth() - checkWidth - knobGap, fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));

        Color knobColor = ColorUtil.interpolate(UIColors.knob(fullAlpha), UIColors.inactiveKnob(fullAlpha), anim);

        RenderUtil.BLUR_RECT.draw(matrixStack, checkX, checkY, checkWidth, checkHeight, checkRound, ColorUtil.setAlpha(color, fullAlpha));
        RenderUtil.BLUR_RECT.draw(matrixStack, knobX, knobY, knobSize, knobSize, knobRound, knobColor);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.toggle();
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}

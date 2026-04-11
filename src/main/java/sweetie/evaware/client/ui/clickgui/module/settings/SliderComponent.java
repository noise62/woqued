package sweetie.evaware.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.clickgui.module.SettingComponent;

import java.awt.*;

public class SliderComponent extends SettingComponent {
    private final SliderSetting setting;
    private boolean dragging;
    private float currentWidth;
    private float previewValue;

    private final AnimationUtil dragAnimation = new AnimationUtil();

    public SliderComponent(SliderSetting setting) {
        super(setting);
        this.setting = setting;
        this.previewValue = setting.getValue();
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        dragAnimation.update();
        dragAnimation.run(dragging ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        MatrixStack matrixStack = context.getMatrices();

        float bigPenis = dragging ? previewValue : setting.getValue();
        float fontSize = fontSize();
        String valueText = String.valueOf(bigPenis);
        float valueWidth = Fonts.PS_MEDIUM.getWidth(valueText, fontSize);
        float piska = scaled(0.5f);

        int fullAlpha = (int) (getAlpha() * 255f);

        float progress = (bigPenis - setting.getMin()) / (setting.getMax() - setting.getMin()) * sliderWidth();
        currentWidth = MathUtil.interpolate(currentWidth, progress, 0.2f);

        Fonts.PS_MEDIUM.drawText(matrixStack, setting.getName(), getX() + piska, getY() + piska, fontSize, UIColors.textColor(fullAlpha));
        Fonts.PS_MEDIUM.drawText(matrixStack, valueText, getX() - piska + getWidth() - valueWidth, getY() + piska, fontSize, UIColors.textColor(fullAlpha));

        float sliderRound = sliderHeight() * 0.3f;
        float knobX = MathHelper.clamp(sliderX() + currentWidth - knobSize() / 2f, sliderX(), sliderX() + sliderWidth() - knobSize());

        Color knobColor = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.knob(), UIColors.inactiveKnob(), dragAnimation.getValue()), fullAlpha);

        float hui = (knobSize() - sliderHeight()) / 2f;

        Color color1 = UIColors.gradient(0, fullAlpha);
        Color color2 = UIColors.gradient(90, fullAlpha);

        RenderUtil.BLUR_RECT.draw(matrixStack, sliderX(), sliderY(), sliderWidth(), sliderHeight(), sliderRound, UIColors.backgroundBlur(fullAlpha));
        RenderUtil.BLUR_RECT.draw(matrixStack, sliderX(), sliderY(), currentWidth, sliderHeight(), new Vector4f(sliderRound), color2, color1, color2, color1);
        RenderUtil.BLUR_RECT.draw(matrixStack, knobX, sliderY() - hui, knobSize(), knobSize(), knobSize() * 0.4f, knobColor);

        setHeight(sliderHeight() + (sliderY() - getY()) + knobSize() / 2f);

        if (dragging) {
            float newValue = (mouseX - getX()) / sliderWidth();
            newValue = setting.getMin() + newValue * (setting.getMax() - setting.getMin());
            newValue = Math.round(newValue / setting.getStep()) * setting.getStep();
            previewValue = MathUtil.round(Math.max(setting.getMin(), Math.min(setting.getMax(), newValue)), setting.getStep());
        }

        if (dragging && !MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.setValue(previewValue);
            dragging = false;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            setting.setValue(previewValue);
        }
        dragging = false;
    }

    private float fontSize() { return scaled(15f) * 0.45f; }
    private float sliderWidth() { return getWidth(); }
    private float sliderHeight() { return scaled(3.5f); }
    private float knobSize() { return sliderHeight() * 1.5f; }
    private float sliderY() { return getY() + fontSize() + knobSize() / 2f; }
    private float sliderX() { return getX(); }
    private float getDefaultHeight() { return fontSize() + gap() + knobSize(); }

    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
}
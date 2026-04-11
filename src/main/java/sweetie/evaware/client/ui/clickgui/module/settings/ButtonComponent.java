package sweetie.evaware.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import sweetie.evaware.api.module.setting.RunSetting;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.clickgui.module.SettingComponent;

import java.awt.*;

public class ButtonComponent extends SettingComponent {
    private final RunSetting setting;

    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public ButtonComponent(RunSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        MatrixStack matrixStack = context.getMatrices();
        int fullAlpha = (int) (getAlpha() * 255f);

        hoverAnimation.update();
        hoverAnimation.run(hovered(mouseX, mouseY) ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        Color buttonColor1 = ColorUtil.interpolate(UIColors.gradient(0, fullAlpha), UIColors.backgroundBlur(fullAlpha), hoverAnimation.getValue());
        Color buttonColor2 = ColorUtil.interpolate(UIColors.gradient(90, fullAlpha), UIColors.backgroundBlur(fullAlpha), hoverAnimation.getValue());

        float fontSize = getHeight() * 0.45f + scaled((float) hoverAnimation.getValue());
        float round = getWidth() * 0.04f;
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), new Vector4f(round), buttonColor1, buttonColor2, buttonColor1, buttonColor2);
        Fonts.PS_MEDIUM.drawCenteredText(matrixStack, setting.getName(), getX() + getWidth() / 2f, getY() + getHeight() / 2f - fontSize / 2f, fontSize, ColorUtil.setAlpha(UIColors.textColor(), fullAlpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered(mouseX, mouseY)) {
            if (setting.getValue() != null) {
                setting.getValue().run();
            }
        }
    }

    private boolean hovered(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()));
    }

    private float getDefaultHeight() {
        return 15f;
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}

package sweetie.evaware.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.module.setting.ColorSetting;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.clickgui.module.ExpandableComponent;
import sweetie.evaware.client.ui.theme.Theme;

import java.awt.*;

import static sweetie.evaware.api.system.interfaces.QuickImports.mc;

public class ColorComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final Theme.ElementColor elementColor;
    private final ColorSetting setting;

    private boolean draggingHue = false;
    private boolean draggingSatBright = false;
    private boolean draggingAlpha = false;

    private float hueCache = 0f;
    private boolean inited;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;
        this.elementColor = null;
        updateHeight(getDefaultHeight());
        initHueCache();
    }

    public ColorComponent(Theme.ElementColor elementColor) {
        super(null);
        this.elementColor = elementColor;
        this.setting = null;
        updateHeight(getDefaultHeight());
    }

    private void initHueCache() {
        if (inited) {
            return;
        }

        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hueCache = hsb[0];

        inited = true;
    }

    private Color getCurrentColor() {
        return setting != null ? setting.getValue() : elementColor.getColor();
    }

    private void setCurrentColor(Color color) {
        if (setting != null) setting.setValue(color);
        else elementColor.setColor(color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack ms = context.getMatrices();
        updateOpen();
        initHueCache();

        if (draggingHue) updateHue(mouseX);
        if (draggingSatBright) updateSatBright(mouseX, mouseY);
        if (draggingAlpha) updateAlpha(mouseX);

        float baseHeight = scaled(getDefaultHeight());
        float fontSize = baseHeight * 0.45f;

        int fullAlpha = (int) (getAlpha() * 255f);

        if (setting != null) {
            Fonts.PS_MEDIUM.drawText(ms, setting.getName(), getX(), getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));

            float previewSize = baseHeight * 0.7f;
            float previewX = getX() + getWidth() - previewSize;
            float previewY = getY() + baseHeight / 2f - previewSize / 2f;
            float previewRound = previewSize * 0.2f;
            RenderUtil.RECT.draw(ms, previewX, previewY, previewSize, previewSize, previewRound, ColorUtil.setAlpha(getCurrentColor(), (int) (getCurrentColor().getAlpha() / 255f * fullAlpha)));
            updateHeight(getDefaultHeight());
        }

        float animValue = getAnimValue();
        if (animValue > 0.0) {
            Color[] colors = getGradientColors(animValue);
            float colorPickerRound = getWidth() * 0.02f;
            RenderUtil.GRADIENT_RECT.draw(ms, getPickerX(), getColorPickerY() + getAnimY(), getPickerWidth(), getColorPickerHeight(), colorPickerRound, colors[0], colors[1], colors[2], colors[3]);

            drawHueBar(ms, animValue);
            drawAlphaBar(ms, animValue);
            drawSelectors(ms);

            float alphaHeight = (getAlphaHeight() + gap());
            float extraHeight = (getHueHeight() + getColorPickerHeight() + alphaHeight + gap()) * animValue;
            float baseHeightFinal = setting != null ? baseHeight : 0f;
            setHeight(baseHeightFinal + extraHeight);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (setting != null && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;

        if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getColorPickerY(), getPickerWidth(), getColorPickerHeight())) {
            draggingSatBright = true;
            updateSatBright(mouseX, mouseY);
        } else if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getHueY(), getPickerWidth(), getHueHeight())) {
            draggingHue = true;
            updateHue(mouseX);
        } else if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getAlphaY(), getPickerWidth(), getAlphaHeight())) {
            draggingAlpha = true;
            updateAlpha(mouseX);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingHue = false;
        draggingSatBright = false;
        draggingAlpha = false;
    }


    // updates
    private void updateHue(double mouseX) {
        float rel = (float) ((mouseX - getPickerX()) / getPickerWidth());
        rel = Math.max(0f, Math.min(1f, rel));
        hueCache = rel;

        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        setCurrentColor(new Color(Color.HSBtoRGB(rel, hsb[1], hsb[2]), true));
    }

    private void updateSatBright(double mouseX, double mouseY) {
        float sat = (float) ((mouseX - getPickerX()) / getPickerWidth());
        float bri = 1f - (float) ((mouseY - getColorPickerY()) / getColorPickerHeight());
        sat = Math.max(0f, Math.min(1f, sat));
        bri = Math.max(0f, Math.min(1f, bri));

        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        float hue = (hsb[1] == 0 || hsb[2] == 0) ? hueCache : hsb[0];
        Color newColor = new Color(Color.HSBtoRGB(hue, sat, bri));
        setCurrentColor(new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), color.getAlpha()));
    }

    private void updateAlpha(double mouseX) {
        float rel = (float) ((mouseX - getPickerX()) / getPickerWidth());
        rel = Math.max(0f, Math.min(1f, rel));
        int alpha = (int) (rel * 255);

        Color c = getCurrentColor();
        setCurrentColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
    }

    // render
    private void drawAlphaBar(MatrixStack ms, float animValue) {
        float y = getAlphaY() + getAnimY();
        float h = getAlphaHeight();

        Color c = getCurrentColor();
        Color left = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
        Color right = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (getAnimValue() * getAlpha() * 255f));

        RenderUtil.GRADIENT_RECT.draw(ms, getPickerX(), y, getPickerWidth(), h, h * 0.3f, left, right, left, right);
    }

    private void drawHueBar(MatrixStack ms, float animValue) {
        float y = getHueY() + getAnimY();
        float h = getAlphaHeight();

        RenderUtil.TEXTURE_RECT.draw(
                ms, getPickerX(), y,
                getPickerWidth(), h, h * 0.3f,
                new Color(255, 255, 255, (int) (getAnimValue() * getAlpha() * 255f)),
                0f, 0f, 1f, 1f,
                mc.getTextureManager().getTexture(FileUtil.getImage("interface/hue")).getGlId()
        );
    }
    private void drawSelectors(MatrixStack ms) {
        int alpha = (int) (getAnimValue() * getAlpha() * 255f);
        Color currentColor = getCurrentColor();
        Color cursorColor = ColorUtil.setAlpha(Color.WHITE, alpha);
        float[] hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        float lineOffset = scaled(4f);
        float lineWidth = lineOffset;
        float lineHeight = lineWidth;
        float lineRound = lineOffset * 0.5f;
        float lineYOffset = getHueHeight() / 2f - lineHeight / 2f;

        float circleOffset = scaled(2f);
        float circleSize = circleOffset * 2f;

        // Sat/Bri selector
        float satX = getPickerX() + hsb[1] * getPickerWidth();
        float briY = getColorPickerY() + (1 - hsb[2]) * getColorPickerHeight();
        RenderUtil.RECT.draw(ms, satX - circleOffset, briY + getAnimY() - circleOffset, circleSize, circleSize, circleSize * 0.5f, cursorColor);

        // Hue selector
        float hueX = getPickerX() + hueCache * getPickerWidth();
        RenderUtil.RECT.draw(ms, hueX - lineOffset, getHueY() + getAnimY() + lineYOffset, lineWidth, lineHeight, lineRound, cursorColor);

        // Alpha selector
        float alphaRel = currentColor.getAlpha() / 255f;
        float alphaX = getPickerX() + alphaRel * getPickerWidth();
        RenderUtil.RECT.draw(ms, alphaX - lineOffset, getAlphaY() + getAnimY() + lineYOffset, lineWidth, lineHeight, lineRound, cursorColor);
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) { }

    // helpers
    private float getAnimY() { return (-gap() * (1f - getAnimValue())); }
    private float getColorPickerY() { return getY() + (setting != null ? scaled(getDefaultHeight()) : 0f); }
    private float getColorPickerHeight() { return getWidth() * getAnimValue() * 0.36f; }
    private float getHueY() { return getColorPickerY() + getColorPickerHeight() + gap(); }
    private float getHueHeight() { return scaled(5f) * getAnimValue(); }
    private float getAlphaY() { return getHueY() + getHueHeight() + gap(); }
    private float getAlphaHeight() { return getHueHeight(); }
    private float getPickerX() { return getX(); }
    private float getPickerWidth() { return getWidth(); }
    private float getDefaultHeight() { return 15f; }
    private float getAnimValue() { return getValue(); }

    private Color[] getGradientColors(float anim) {
        int alpha = (int) (anim * getAlpha() * 255f);
        Color topLeft = ColorUtil.setAlpha(Color.WHITE, alpha);
        Color bottom = ColorUtil.setAlpha(Color.BLACK, alpha);

        float hue = hueCache;
        Color hueColor = new Color(Color.HSBtoRGB(hue, 1f, 1f));
        Color topRight = ColorUtil.setAlpha(hueColor, alpha);

        return new Color[]{topLeft, topRight, bottom, bottom};
    }
}

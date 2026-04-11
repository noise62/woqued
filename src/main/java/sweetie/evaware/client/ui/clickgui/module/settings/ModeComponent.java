package sweetie.evaware.client.ui.clickgui.module.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.ScissorUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.clickgui.module.ExpandableComponent;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final ModeSetting setting;

    private final List<Bound> bounds = new ArrayList<>();
    private final Map<String, AnimationUtil> modeAnimations = new HashMap<>();

    public ModeComponent(ModeSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());

        for (String mode : setting.getModes()) {
            modeAnimations.put(mode, new AnimationUtil());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        updateOpen();

        float fontSize = getDefaultHeight() * scaled(0.45f);
        float scd = scaled(getDefaultHeight());
        float zavoz = offset();
        float anim = getValue();

        String valueText = setting.getValue();
        String name = setting.getName();
        float valueWidth = Fonts.PS_MEDIUM.getWidth(valueText, fontSize);
        float nameWidth = Fonts.PS_BOLD.getWidth(name, fontSize);

        float nameX = (getWidth() / 2f - nameWidth / 2f) * anim + zavoz * (1f - anim);
        int fullAlpha = (int) (getAlpha() * 255f);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), getWidth() * 0.04f, UIColors.backgroundBlur(fullAlpha));
        Fonts.PS_BOLD.drawWrap(matrixStack, name, getX() + nameX, getY() + scd / 2f - fontSize / 2f, getWidth() - zavoz * 2f - valueWidth * (1f - anim), fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));


        ScissorUtil.start(matrixStack, getX(), getY(), getWidth(), getHeight());
        Fonts.PS_MEDIUM.drawText(matrixStack, valueText, getX() + getWidth() - zavoz - valueWidth * (1f - anim), getY() + scd / 2f - fontSize / 2f, fontSize, UIColors.primary((int) ((1f - anim) * getAlpha() * 255f)));

        if (anim > 0.0) {
            float bY = -scaled(2f) * (1f - anim);
            bounds.clear();
            float defX = getX() + zavoz;
            float currentX = defX;
            float currentY = getY() + scd + bY;
            float tileSize = fontSize * 0.9f;
            float tileHeight = tileSize * 1.8f;
            float tilePadding = gap();

            fullAlpha = (int) (getAlpha() * anim * 255f);

            RenderUtil.OTHER.scaleStart(matrixStack, getX() + getWidth() / 2f, getY() + getDefaultHeight() + getHeight() / 2f - bY, 0.95f + (0.05f * anim));

            for (String mode : setting.getModes()) {
                AnimationUtil modeAnim = modeAnimations.get(mode);

                modeAnim.update();
                modeAnim.run(setting.is(mode) ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

                float textWidth = Fonts.PS_MEDIUM.getWidth(mode, tileSize);
                float tileWidth = textWidth + tileSize;

                if (currentX + tileWidth + tilePadding > getX() + getWidth()) {
                    currentX = defX;
                    currentY += tileHeight + tilePadding;
                }

                bounds.add(new Bound(currentX, currentY, tileWidth, tileHeight, mode));

                Color rectColor = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.primary(), UIColors.widgetBlur(), modeAnim.getValue()), fullAlpha);

                RenderUtil.BLUR_RECT.draw(matrixStack, currentX, currentY, tileWidth, tileHeight, tileHeight * 0.2f, rectColor);
                Fonts.PS_MEDIUM.drawCenteredText(matrixStack, mode, currentX + tileWidth / 2f, currentY + tileHeight / 2f - tileSize / 2f, tileSize, UIColors.textColor(fullAlpha));

                currentX += tileWidth + tilePadding;
            }

            RenderUtil.OTHER.scaleStop(matrixStack);

            float total = (currentY - getY() + tileHeight) * anim;
            float impotentMan = Math.max(total, scd + tileHeight * anim);
            float jopa = gap() * (anim * 2f);
            setHeight(impotentMan + jopa);
        } else {
            updateHeight(getDefaultHeight());
        }

        ScissorUtil.stop(matrixStack);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;
        for (Bound bound : bounds) {
            if (MouseUtil.isHovered(mouseX, mouseY, bound.x, bound.y, bound.width, bound.height)) {
                setting.setValue(bound.value);
            }
        }
    }

    private float getDefaultHeight() {
        return 15f;
    }

    private record Bound(float x, float y, float width, float height, String value) {}

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}

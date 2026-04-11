package sweetie.evaware.client.ui.widget;

import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Icons;

public abstract class InformationWidget extends Widget {
    public InformationWidget(float x, float y) {
        super(x, y);
    }

    @Override
    public void render(MatrixStack matrixStack) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        String valueText = " " + getValue();

        float fontSize = scaled(7.5f);
        float nameWidth = getSemiBoldFont().getWidth(getName(), fontSize);
        float valueWidth = getSemiBoldFont().getWidth(valueText, fontSize);

        float backgroundWidth = nameWidth + valueWidth + getGap() * 2f;
        float backgroundHeight = fontSize + getGap() * 2f;
        float round = backgroundHeight * 0.3f;

        float textX = x + getGap();
        float textY = y + getGap();

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, UIColors.widgetBlur());
        getSemiBoldFont().drawGradientText(matrixStack, getName(), textX, textY, fontSize, UIColors.primary(), UIColors.secondary(), nameWidth / 4f);
        getSemiBoldFont().drawText(matrixStack, valueText, textX + nameWidth, textY, fontSize, UIColors.textColor());

        getDraggable().setWidth(backgroundWidth);
        getDraggable().setHeight(backgroundHeight);
    }

    public abstract String getValue();
    public abstract Icons getIcon();
}
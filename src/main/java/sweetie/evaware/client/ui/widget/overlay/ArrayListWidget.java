package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.Color;
import java.util.List;

public class ArrayListWidget extends Widget {

    public ArrayListWidget() {
        super(2f, 30f);
    }

    @Override
    public String getName() {
        return "ArrayList";
    }

    @Override
    public void render(MatrixStack ms) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();

        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float fontSize = scaled(6);
        float paddingH = scaled(2.5f);
        float paddingV = scaled(1.2f);
        float stripeWidth = 1.5f;

        List<Module> enabledModules = ModuleManager.getInstance().getModules().stream()
                .filter(Module::isEnabled)
                .sorted((m1, m2) -> {
                    float w1 = getMediumFont().getWidth(m1.getName(), fontSize);
                    float w2 = getMediumFont().getWidth(m2.getName(), fontSize);
                    return Float.compare(w2, w1);
                })
                .toList();

        float currentY = y;
        float maxSeenWidth = 0;

        Color bg = new Color(12, 12, 18, 240);

        for (Module module : enabledModules) {
            String moduleName = module.getName();
            float textWidth = getMediumFont().getWidth(moduleName, fontSize);

            float rectWidth = textWidth + (paddingH * 2) + stripeWidth;
            float rectHeight = fontSize + (paddingV * 2);

            float moduleX = isRightSide ? (x + width - rectWidth) : x;

            RenderUtil.RECT.draw(ms, moduleX, currentY, rectWidth, rectHeight, 0, bg);

            float stripeX = isRightSide ? (moduleX + rectWidth - stripeWidth) : moduleX;
            RenderUtil.GRADIENT_RECT.draw(ms,
                    stripeX, currentY, stripeWidth, rectHeight, 0f,
                    UIColors.primary(), UIColors.primary(),
                    UIColors.secondary(), UIColors.secondary()
            );

            float textX = isRightSide ? (moduleX + paddingH) : (moduleX + stripeWidth + paddingH);

            getMediumFont().drawGradientText(
                    ms, moduleName, textX, currentY + paddingV + 0.5f,
                    fontSize, UIColors.primary(), UIColors.secondary(), textWidth
            );

            if (rectWidth > maxSeenWidth) maxSeenWidth = rectWidth;
            currentY += rectHeight;
        }

        getDraggable().setWidth(maxSeenWidth);
        getDraggable().setHeight(enabledModules.isEmpty() ? scaled(10) : (currentY - y));
    }
}
package sweetie.evaware.client.ui.widget;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.ScissorUtil;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
@Setter
public abstract class ContainerWidget extends Widget {
    public ContainerWidget(float x, float y) {
        super(x, y);
    }

    private final List<ContainerElement> activeElements = new ArrayList<>();

    private final AnimationUtil showAnimation = new AnimationUtil();
    private final AnimationUtil widthAnimation = new AnimationUtil();
    private final AnimationUtil heightAnimation = new AnimationUtil();
    private float width, height, containerHeight;

    public boolean shouldShow() {
        return getActiveElements().stream().anyMatch(e -> e.getAnimation().getValue() > 0.1);
    }

    public List<ContainerElement> containerElements() {
        updateActiveElements();
        return getActiveElements();
    }

    protected abstract Map<String, ContainerElement.ColoredString> getCurrentData();

    protected boolean shouldKeep(ContainerElement element, Map<String, ContainerElement.ColoredString> currentData) {
        return currentData.containsKey(element.getFirst());
    }

    private float getOffset() { return getGap() * 1.3f; }
    private float getDefaultHeight() { return getFontSize(true) + getGap() * 3f; }
    private int fullAlpha() { return (int) (showAnimation.getValue() * 255f); }
    private float getFontSize(boolean header) { return scaled(header ? 8f : 7f); }

    @Override
    public void render(MatrixStack matrixStack) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        widthAnimation.update();
        heightAnimation.update();
        showAnimation.update();

        showAnimation.run(shouldShow() || mc.currentScreen instanceof ChatScreen ? 1.0 : 0.0, getDuration(), getEasing());

        float animHeight = (float) heightAnimation.getValue();
        float animWidth = (float) widthAnimation.getValue();
        float offset = getOffset() * 1.2f;
        float round = getGap() * 2f;

        int alpha = fullAlpha();

        float headerSize = getFontSize(true);
        float headerWidth = getMediumFont().getWidth(getName(), headerSize);

        width = headerWidth + getOffset() * 4f;
        height = getDefaultHeight();

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, animWidth, animHeight, round, UIColors.widgetBlur(alpha));
        getMediumFont().drawCenteredGradientText(matrixStack, getName(), x + animWidth / 2f, y + getDefaultHeight() / 2f - headerSize / 2f, headerSize, UIColors.primary(alpha), UIColors.secondary(alpha), headerWidth / 4f);

        ScissorUtil.start(matrixStack, x, y, animWidth, animHeight);

        containerHeight = 0f;
        float elementY = y + getDefaultHeight() + offset / 2f;
        float elementSize = getFontSize(false);
        float xFirst = x + offset;
        float xSecondBase = x + animWidth - offset;

        List<ContainerElement> elements = containerElements();
        for (ContainerElement containerElement : elements) {
            String first = containerElement.getFirst();
            String second = containerElement.getSecond().text();
            float anim = (float) containerElement.getAnimation().getValue();
            float sex = scaled(2f) * (1f - anim);
            int elementAlpha = (int) (anim * showAnimation.getValue() * 255f);
            getMediumFont().drawText(matrixStack, first, xFirst, elementY - sex, elementSize, UIColors.textColor(elementAlpha));

            float secondWidth = getMediumFont().getWidth(second, elementSize);
            getMediumFont().drawText(matrixStack, second, xSecondBase - secondWidth, elementY - sex, elementSize, ColorUtil.setAlpha(containerElement.getSecond().color, elementAlpha));

            float addition = elementSize + getGap();
            elementY += addition * anim;

            if (containerElement.isValid()) addHeight(addition);

            updateWidth(first, second, offset * 4f);
        }

        ScissorUtil.stop(matrixStack);

        widthAnimation.run(width, getDuration(), getEasing());
        heightAnimation.run(height, getDuration(), getEasing());

        getDraggable().setWidth((float) widthAnimation.getValue());
        getDraggable().setHeight((float) heightAnimation.getValue());
    }

    public void addHeight(float value) {
        containerHeight += value;
        height = getDefaultHeight() + containerHeight + getOffset();
    }

    public void updateWidth(String first, String second, float gap) {
        float fontSize = getFontSize(false);
        float firstWidth = getMediumFont().getWidth(first, fontSize);
        float secondWidth = getMediumFont().getWidth(second, fontSize);
        float total = firstWidth + gap + secondWidth;
        if (total > width) {
            width = total;
        }
    }

    private void updateActiveElements() {
        Easing easing = getEasing();
        long duration = getDuration();

        for (ContainerElement element : activeElements) {
            element.getAnimation().update();
        }

        Map<String, ContainerElement.ColoredString> currentData = getCurrentData();
        if (currentData == null) currentData = Collections.emptyMap();

        Map<String, ContainerElement> lookup = new HashMap<>(activeElements.size() * 2 + 1);
        for (ContainerElement e : activeElements) {
            lookup.put(e.getFirst(), e);
        }

        for (Map.Entry<String, ContainerElement.ColoredString> entry : currentData.entrySet()) {
            String name = entry.getKey();
            ContainerElement.ColoredString value = entry.getValue();

            ContainerElement element = lookup.get(name);
            if (element == null) {
                ContainerElement newElement = new ContainerElement(name, value);
                newElement.getAnimation().run(1.0, duration, easing);
                activeElements.add(newElement);
                lookup.put(name, newElement);
            } else {
                if (!element.getSecond().equals(value)) {
                    element.setSecond(value);
                }
                element.setValid(true);
            }
        }

        Iterator<ContainerElement> iterator = activeElements.iterator();
        while (iterator.hasNext()) {
            ContainerElement element = iterator.next();
            AnimationUtil anim = element.getAnimation();

            if (!shouldKeep(element, currentData)) {
                element.setValid(false);
                anim.run(0.0, duration, easing);
                if (anim.getValue() <= 0.1) {
                    iterator.remove();
                }
            } else if (element.isValid()) {
                anim.run(1.0, duration, easing);
            }
        }
    }

    @Setter
    @Getter
    public static class ContainerElement {
        private String first;
        private ColoredString second;
        private boolean valid = true;

        private final AnimationUtil animation = new AnimationUtil();

        public ContainerElement(String first, ColoredString second) {
            this.first = first;
            this.second = second;
        }

        @Getter
        @Accessors(fluent = true)
        public static class ColoredString {
            private final String text;
            private final Color color;

            public ColoredString(String text, Color color) {
                this.text = text;
                this.color = color;
            }

            public ColoredString(String text) {
                this(text, UIColors.textColor());
            }
        }
    }
}

package sweetie.evaware.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.other.WindowResizeEvent;
import sweetie.evaware.api.system.configs.ThemeManager;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MouseUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.api.utils.render.fonts.Icons;
import sweetie.evaware.client.ui.UIComponent;
import sweetie.evaware.client.ui.clickgui.module.settings.ColorComponent;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ThemeEditor extends UIComponent {
    @Getter private static final ThemeEditor instance = new ThemeEditor();

    private final List<ThemeSelectable> themeSelectables = new ArrayList<>();
    private final List<ThemeBound> themeBounds = new ArrayList<>();

    private final Theme defaultTheme = new Theme("Woqued");
    private Theme currentTheme = defaultTheme;
    protected Theme editTheme;

    private final String placeHolderText = "Enter name...";
    private String typingText = "";
    private boolean typing;

    private DeleteButton deleteButton = new DeleteButton(-1f, -1f, -1f);

    private final AnimationUtil openAnimation = new AnimationUtil();
    @Setter private boolean open;
    @Setter private float anim;

    private int alphaAnim() {
        return (int) (openAnimation.getValue() * anim * 255);
    }

    private record ThemeBound(float x, float y, float width, float height, Theme.ElementColor elementColor) { }
    private record DeleteButton(float x, float y, float size) {}

    public ThemeEditor() {
        setWidth(scaled(95f));
        setHeight(scaled(150f));

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            setWidth(scaled(95f));
        }));
    }

    public void init() {
        refresh();
    }

    private void refresh() {
        ThemeManager.getInstance().refresh();
    }

    public void save(boolean last) {
        if (!last) {
            ThemeManager.getInstance().saveAll();
        } else if (currentTheme != null) {
            ThemeManager.getInstance().saveLastSelected(currentTheme);
        }
    }

    public void load() {
        ThemeManager.getInstance().refresh();
        Theme last = ThemeManager.getInstance().loadLastSelected();
        if (last != null) {
            themeSelectables.add(new ThemeSelectable(last));
            currentTheme = last;
        } else {
            themeSelectables.add(new ThemeSelectable(defaultTheme));
            currentTheme = defaultTheme;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 100, Easing.SINE_OUT);
        if (openAnimation.getValue() <= 0.1) return;

        themeBounds.clear();

        //System.out.println(currentTheme.getName());
        //for (Theme.ElementColor elementColor : currentTheme.getElementColors()) { System.out.println(elementColor.getName() + ": " + elementColor.getColor()); }

        float round = getWidth() * 0.05f;
        float headerHeight = scaled(getHeaderHeight());
        float headerFontSize = headerHeight * 0.52f;
        String text = "Theme Editor";
        float textWidth = Fonts.PS_BOLD.getWidth(text, headerFontSize);
        setWidth(textWidth * 1.5f);

        MatrixStack matrixStack = context.getMatrices();

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), round, UIColors.blur(alphaAnim()));
        Fonts.PS_BOLD.drawGradientText(matrixStack, text, getX() + getWidth() / 2f - textWidth / 2f, getY() + headerHeight / 2f - headerFontSize / 2f, headerFontSize, UIColors.primary(alphaAnim()), UIColors.secondary(alphaAnim()), Fonts.PS_BOLD.getWidth(text, headerFontSize) / 4f);

        placeRender(context, mouseX, mouseY, delta);

        float xOffset = getX() + offset();
        float widthOffset = getWidth() - offset() * 2f;

        float themeY = gap() + headerHeight + getPlaceTextCoordinates()[3];

        if (editTheme == null) {
            for (ThemeSelectable theme : themeSelectables) {
                theme.setAlpha((float) (openAnimation.getValue() * anim));
                theme.setX(xOffset);
                theme.setY(getY() + themeY);
                theme.setWidth(widthOffset);
                theme.setHeight(scaled(17f));

                theme.render(context, mouseX, mouseY, delta);

                themeY += theme.getHeight() + gap();
            }

            setHeight(themeY);
        } else {
            float elementY = themeY;
            float elementHeight = scaled(getElementHeight());

            float textSize = elementHeight * 0.6f;
            float textY = getY() + elementHeight / 2f - textSize / 2f;

            float elementWidth = getWidth() - offset();

            float colorSize = elementHeight * 0.8f;
            float colorX = getX() + elementWidth - colorSize;
            float colorY = getY() + elementHeight / 2f - colorSize / 2f;

            float roundColor = colorSize * 0.2f;

            for (Theme.ElementColor elementColor : editTheme.getElementColors()) {
                float height = elementHeight;
                Fonts.PS_MEDIUM.drawText(matrixStack, elementColor.getName(), xOffset, textY + elementY, textSize, UIColors.textColor(alphaAnim()));
                RenderUtil.RECT.draw(matrixStack, colorX, colorY + elementY, colorSize, colorSize, roundColor, ColorUtil.setAlpha(elementColor.getColor(), alphaAnim()));

                ColorComponent colorComponent = elementColor.getColorComponent();
                colorComponent.updateOpen();
                float cAnim = colorComponent.getValue();

                if (cAnim > 0.0) {
                    colorComponent.setX(xOffset);
                    colorComponent.setY(textY + elementY + elementHeight);
                    colorComponent.setWidth(widthOffset);
                    colorComponent.setAlpha(alphaAnim() / 255f);
                    colorComponent.render(context, mouseX, mouseY, delta);

                    height += colorComponent.getHeight() + (gap() * 2f) * cAnim;
                }

                themeBounds.add(new ThemeBound(xOffset, colorY + elementY, elementWidth, height, elementColor));

                elementY += height + gap();
            }

            setHeight(elementY);
        }
    }

    private void placeRender(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        float[] placeCoords = getPlaceTextCoordinates();
        float placeX = placeCoords[0];
        float placeY = placeCoords[1];
        float placeWidth = placeCoords[2];
        float placeHeight = placeCoords[3];
        float placeFontSize = placeHeight * 0.4f;
        float placeRound = placeHeight * 0.2f;

        String cursor = typing && System.currentTimeMillis() % 1000 > 500 ? "_" : " ";
        String typeText = typingText.isEmpty() && !typing ? placeHolderText : typingText + cursor;

        boolean edit = editTheme != null;

        if (edit) {
            typeText = editTheme.getName();
        }

        RenderUtil.BLUR_RECT.draw(matrixStack, placeX, placeY, placeWidth, placeHeight, placeRound, UIColors.widgetBlur(alphaAnim()));
        Fonts.PS_BOLD.drawText(matrixStack, typeText, placeX + offset(), placeY + placeHeight / 2f - placeFontSize / 2f, placeFontSize, UIColors.textColor(alphaAnim()));

        if (edit && themeSelectables.size() > 1) {
            float margin = gap();
            float deleteSize = placeHeight - margin * 2f;
            float deleteX = placeX + getWidth() - deleteSize - margin - offset() * 2f;
            float deleteY = placeY + margin;
            float iconSize = deleteSize * 0.5f;
            RenderUtil.BLUR_RECT.draw(matrixStack, deleteX, deleteY, deleteSize, deleteSize, placeRound, UIColors.blur(alphaAnim()));
            Fonts.ICONS.drawCenteredText(matrixStack, Icons.CROSS.getLetter(), deleteX + deleteSize / 2f, deleteY + deleteSize / 2f - iconSize / 2f, iconSize, UIColors.textColor(alphaAnim()), 0.1f);

            deleteButton = new DeleteButton(deleteX, deleteY, deleteSize);
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open) return;

        if (typing && editTheme == null) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!typingText.isEmpty()) {
                        typingText = typingText.substring(0, typingText.length() - 1);
                    }
                }

                case GLFW.GLFW_KEY_ENTER -> {
                    if (!typingText.isEmpty()) {
                        boolean exists = themeSelectables.stream().anyMatch(ts -> ts.getTheme().getName().equalsIgnoreCase(typingText));

                        if (!exists) {
                            Theme newTheme = new Theme(ThemeManager.getInstance().safeFileName(typingText));
                            newTheme.getElementColors().clear();
                            for (Theme.ElementColor elementColor : currentTheme.getElementColors()) {
                                newTheme.getElementColors().add(new Theme.ElementColor(
                                        elementColor.getName(),
                                        elementColor.getColor()
                                ));
                            }

                            themeSelectables.add(new ThemeSelectable(newTheme));
                            currentTheme = newTheme;
                        }

                        typingText = "";
                        typing = false;
                    }
                }
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return;

        if (editTheme != null) {
            if (themeSelectables.size() > 1 && MouseUtil.isHovered(mouseX, mouseY, deleteButton.x, deleteButton.y, deleteButton.size, deleteButton.size)) {

                if (themeSelectables.removeIf(ts -> ts.getTheme() == editTheme)) {
                    ThemeManager.getInstance().remove(editTheme.getName());
                    editTheme = null;
                }

                return;
            }

            if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getHeaderHeight()))) {
                editTheme = null;
                return;
            }

            for (ThemeBound themeBound : themeBounds) {
                ColorComponent colorComponent = themeBound.elementColor.getColorComponent();
                if (MouseUtil.isHovered(mouseX, mouseY, themeBound.x, themeBound.y, themeBound.width, scaled(getElementHeight()))) {
                    colorComponent.toggleOpen();
                    return;
                }
                if (MouseUtil.isHovered(mouseX, mouseY, themeBound.x, themeBound.y, themeBound.width, themeBound.height)) {
                    colorComponent.mouseClicked(mouseX, mouseY, button);
                }
            }
        } else {
            if (MouseUtil.isHovered(mouseX, mouseY, getPlaceTextCoordinates()[0], getPlaceTextCoordinates()[1], getPlaceTextCoordinates()[2], getPlaceTextCoordinates()[3])) {
                typing = !typing;
                return;
            }
            for (ThemeSelectable theme : themeSelectables) {
                if (MouseUtil.isHovered(mouseX, mouseY, theme.getX(), theme.getY(), theme.getWidth(), theme.getHeight())) {
                    if (button == 1) {
                        editTheme = theme.getTheme();
                    } else if (button == 0) {
                        currentTheme = theme.getTheme();
                        ThemeManager.getInstance().saveLastSelected(currentTheme);
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!open) return;

        if (editTheme != null) {
            for (ThemeBound themeBound : themeBounds) {
                themeBound.elementColor.getColorComponent().mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!open) return false;

        if (typing && typingText.length() < 10) {
            typingText += chr;
            return true;
        }
        return false;
    }

    private float[] getPlaceTextCoordinates() {
        float x = getX() + offset();
        float y = getY() + scaled(getHeaderHeight());
        float width = getWidth() - offset() * 2f;
        float height = scaled(getTypingFieldHeight());
        return new float[]{x, y, width, height};
    }
    private float getElementHeight() {
        return 12f;
    }
    private float getHeaderHeight() {
        return 19f;
    }
    private float getTypingFieldHeight() {
        return 19f;
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

    }
}

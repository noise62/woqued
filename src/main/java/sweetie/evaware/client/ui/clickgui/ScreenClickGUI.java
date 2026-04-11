package sweetie.evaware.client.ui.clickgui;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.client.services.RenderService;
import sweetie.evaware.client.ui.theme.ThemeEditor;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ScreenClickGUI extends Screen implements QuickImports {
    @Getter private static final ScreenClickGUI instance = new ScreenClickGUI();

    private float scroll;
    private final AnimationUtil scrollAnimation = new AnimationUtil();

    private boolean open;
    private final AnimationUtil openAnimation = new AnimationUtil();

    private final List<Panel> panels = new ArrayList<>();
    private final ThemeEditor themeEditor = ThemeEditor.getInstance();

    public ScreenClickGUI() {
        super(Text.of(""));

        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            Panel panel = new Panel(category);
            panel.setCategoryIndex(i * 45);
            panels.add(panel);
        }
    }

    @Override
    public void close() {
        ThemeEditor.getInstance().save(false);

        open = false;

        super.close();
    }

    @Override
    protected void init() {
        ThemeEditor.getInstance().init();

        open = true;

        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        scrollAnimation.update();
        scrollAnimation.run(scroll, 600, Easing.EXPO_OUT);

        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        float openAnim = (float) openAnimation.getValue();
        if (!open && openAnim < 0.1) close();

        float windowHeight = mc.getWindow().getScaledHeight();
        float windowWidth = mc.getWindow().getScaledWidth();
        float off = RenderService.getInstance().scaled(12f);
        float totalWidth = panels.stream().map(Panel::getWidth).reduce(0f, Float::sum) + (panels.size() - 1) * (off / 2f);

        float sex = !open ? 1f - openAnim : (-1f + openAnim);
        float panelY = (float) (windowHeight / 12f + scrollAnimation.getValue()) * openAnim + (windowHeight * sex);
        float firstX = (windowWidth - totalWidth) / 2f;

        if (themeEditor.isOpen()) {
            themeEditor.setAnim(openAnim);
            themeEditor.setX(windowWidth - themeEditor.getWidth() * themeEditor.getAnim());
            themeEditor.setY(windowHeight / 2f - themeEditor.getHeight() / 2f);
        }

        for (Panel panel : panels) {
            panel.setAlpha(openAnim);
            panel.setY(panelY);
            panel.setX((firstX + panels.indexOf(panel) * (panel.getWidth() + (off / 2f))));

            panel.render(context, mouseX, mouseY, delta);
        }

        themeEditor.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            open = false;
            mc.mouse.lockCursor();
            return true;
        }

        panels.forEach(panel -> panel.keyPressed(keyCode, scanCode, modifiers));
        themeEditor.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        panels.forEach(panel -> panel.mouseClicked(mouseX, mouseY, button));
        themeEditor.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        panels.forEach(panel -> panel.mouseReleased(mouseX, mouseY, button));
        themeEditor.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float amout = RenderService.getInstance().scaled(15f);
        scroll -= (float) (verticalAmount * amout);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return themeEditor.charTyped(chr, modifiers);
    }

    @Override public void blur() {}
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public boolean shouldPause() { return false; }
    @Override protected void applyBlur() {}
}

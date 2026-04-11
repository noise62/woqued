package sweetie.evaware.client.ui.autobuy;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import sweetie.evaware.api.system.interfaces.QuickImports;

public class ScreenAutoBuy extends Screen implements QuickImports {
    @Getter private static final ScreenAutoBuy instance = new ScreenAutoBuy();

    protected ScreenAutoBuy() {
        super(Text.of(""));
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    @Override public void blur() {}
    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public boolean shouldPause() { return false; }
    @Override protected void applyBlur() {}
}

package sweetie.evaware.api.system.interfaces;

import net.minecraft.client.gui.DrawContext;

public interface UIApi {
    void render(DrawContext context, int mouseX, int mouseY, float delta);
    void keyPressed(int keyCode, int scanCode, int modifiers);
    void mouseClicked(double mouseX, double mouseY, int button);
    void mouseReleased(double mouseX, double mouseY, int button);
    void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount);
}

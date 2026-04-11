package sweetie.evaware.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import sweetie.evaware.api.event.events.other.ScreenEvent;
import sweetie.evaware.api.system.backend.Pair;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.player.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ScreenUtil implements QuickImports {
    public void drawButton(ScreenEvent.ScreenEventData event) {
        int buttonWidth = 80;
        int buttonHeight = 20;
        Screen screen = event.screen();

        if (screen instanceof InventoryScreen inv) {
            int x = (screen.width - buttonWidth) / 2;
            int y = (screen.height - inv.backgroundHeight) / 2 - buttonHeight - 5;

            addButtons(event, x, y, buttonWidth, buttonHeight, new Pair<>("Drop all", () -> drop(inv)));
        } else if (screen instanceof HandledScreen<?> handledScreen
                && handledScreen.getScreenHandler() instanceof GenericContainerScreenHandler genericHandler) {

            String title = handledScreen.getTitle().getString();
            if (title.contains("Аукцион") && title.contains("Поиск")) return;

            int rows = genericHandler.getRows();
            int backgroundHeight = 114 + rows * 18;

            int buttonX = screen.width / 2 + 90;
            int buttonY = (screen.height - backgroundHeight) / 2;

            addButtons(event, buttonX, buttonY, buttonWidth, buttonHeight,
                    new Pair<>("Steal all", () -> toInv(genericHandler)),
                    new Pair<>("From inv", () -> fromInv(genericHandler))
                    //new Pair<>("Drop all", () -> drop(handledScreen))
                    );
        }
    }

    @SafeVarargs
    private void addButtons(ScreenEvent.ScreenEventData event, int x, int y, int width, int height, Pair<String, Runnable>... pairs) {
        List<ButtonWidget> buttons = new ArrayList<>();

        int offsetY = y;
        for (Pair<String, Runnable> pair : pairs) {
            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(pair.left()),
                    b -> pair.right().run()
            ).dimensions(x, offsetY, width, height).build();

            buttons.add(button);
            offsetY += height + 5;
        }

        event.buttons().addAll(buttons);
    }

    private void toInv(GenericContainerScreenHandler containerHandler) {
        for (int i = 0; i < containerHandler.getInventory().size(); i++) {
            Slot slot = containerHandler.getSlot(i);
            if (slot.hasStack()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
    }

    private void fromInv(GenericContainerScreenHandler containerHandler) {
        for (int i = containerHandler.getInventory().size(); i < containerHandler.slots.size(); i++) {
            Slot slot = containerHandler.getSlot(i);
            if (slot.hasStack()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
    }

    private void drop(HandledScreen<?> screen){
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            InventoryUtil.dropSlot(slot.id);
        }
    }
}

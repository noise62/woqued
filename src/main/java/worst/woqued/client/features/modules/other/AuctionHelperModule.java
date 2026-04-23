package worst.woqued.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.auction.ParseModeChoice;
import worst.woqued.api.utils.auction.PriceParser;
import worst.woqued.api.utils.render.RenderUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Auction Helper", category = Category.OTHER)
public class AuctionHelperModule extends Module {
    @Getter private static final AuctionHelperModule instance = new AuctionHelperModule();

    private final PriceParser priceParser = new PriceParser();

    @Getter private final ModeSetting mode = new ModeSetting("Mode")
            .value(priceParser.currentMode)
            .values(ParseModeChoice.values()).onAction(() -> {
                priceParser.currentMode = switch (getMode().getValue()) {
                    case "Spooky Time" -> ParseModeChoice.SPOOKY_TIME;
                    case "Holy World" -> ParseModeChoice.HOLY_WORLD;
                    case "Really World" -> ParseModeChoice.REALLY_WORLD;
                    default -> ParseModeChoice.FUN_TIME;
                };
            });
    private final SliderSetting slots = new SliderSetting("Slots").value(3f).range(1f, 6f).step(1f);
    private final List<Slot> minPriceSlots = new ArrayList<>();

    public AuctionHelperModule() {
        addSettings(mode, slots);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handleUpdateEvent();
        }));

        addEvents(updateEvent);
    }

    public void handleUpdateEvent() {
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest)) return;
        String title = mc.currentScreen.getTitle().getString();

        if (!title.contains("Аукцион") && !title.contains("Поиск") && !title.contains("Маркет") && !title.contains("ꈁꀀꈂꌲꈂꀁ") && !title.contains("[☃] Аукционы")) return;

        minPriceSlots.clear();
        minPriceSlots.addAll(getMinPriceSlots(chest));
    }

    private List<Slot> getMinPriceSlots(GenericContainerScreenHandler chest) {
        return chest.slots.stream()
                .filter(s -> s.id <= 44 && !s.getStack().isEmpty() && getPrice(s.getStack()) != -1)
                .sorted((s1, s2) -> Integer.compare(getPrice(s1.getStack()), getPrice(s2.getStack())))
                .limit(slots.getValue().intValue())
                .toList();
    }

    private int getPrice(ItemStack stack) {
        return priceParser.getPrice(stack);
    }

    public void onRenderChest(DrawContext context, Slot slot) {
        if (minPriceSlots.contains(slot)) {
            int alpha = (int)(1 + 110 * Math.abs(Math.sin(System.currentTimeMillis() * 0.005)));
            RenderUtil.RECT.draw(context.getMatrices(), slot.x, slot.y, 16, 16, 0, new Color(0, 255, 0, alpha));
        }
    }
}

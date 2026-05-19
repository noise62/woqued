package worst.woqued.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.utils.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "Auction Helper", category = Category.OTHER)
public class AuctionHelperModule extends Module {
    @Getter private static final AuctionHelperModule instance = new AuctionHelperModule();

    static final Pattern PRICE_PATTERN = Pattern.compile("Цен[аaAАыЫ]?:?\\s*([\\d,\\s\\.]+)", Pattern.CASE_INSENSITIVE);

    static final int CHEAPEST_COLOR = 0xFF4BFF4B;
    static final int BEST_VALUE_COLOR = 0xFF33AAFF;

    private final BooleanSetting onlyMending = new BooleanSetting("Only Mending").value(false);

    private Slot cheapestSlot;
    private Slot bestValueSlot;

    private int lastUpdateTick = 0;
    private int lastSlotCount = 0;

    public AuctionHelperModule() {
        addSettings(onlyMending);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                cheapestSlot = null;
                bestValueSlot = null;
                return;
            }

            int currentSlotCount = screen.getScreenHandler().slots.size();

            if (currentSlotCount != lastSlotCount || mc.player.age - lastUpdateTick > 5) {
                lastUpdateTick = mc.player.age;
                lastSlotCount = currentSlotCount;
                updateBestSlots(screen);
            }
        }));

        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

            DrawContext context = event.context();

            long time = System.currentTimeMillis();

            if (cheapestSlot != null && isValidSlot(cheapestSlot, screen)) {
                int color = getBlinkingColor(CHEAPEST_COLOR, time, 500);
                highlightSlot(context, cheapestSlot, color);
            }

            if (bestValueSlot != null && isValidSlot(bestValueSlot, screen) && bestValueSlot != cheapestSlot) {
                int color = getBlinkingColor(BEST_VALUE_COLOR, time, 600);
                highlightSlot(context, bestValueSlot, color);
            }
        }));

        addEvents(updateEvent, render2DEvent);
    }

    private boolean isValidSlot(Slot slot, GenericContainerScreen screen) {
        if (slot == null) return false;
        if (slot.id < 0 || slot.id >= screen.getScreenHandler().slots.size()) return false;
        Slot currentSlot = screen.getScreenHandler().getSlot(slot.id);
        return currentSlot.hasStack() && !currentSlot.getStack().isEmpty();
    }

    private void updateBestSlots(GenericContainerScreen screen) {
        List<Slot> slots = screen.getScreenHandler().slots;
        List<ItemPriceData> validItems = new ArrayList<>();

        for (Slot slot : slots) {
            if (slot.inventory == mc.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            if (onlyMending.getValue()) {
                if (!hasMending(stack)) continue;
            }

            int totalPrice = parsePriceFromLore(stack);
            if (totalPrice <= 0) continue;

            int count = stack.getCount();
            int pricePerItem = totalPrice / count;

            validItems.add(new ItemPriceData(slot, totalPrice, pricePerItem, count));
        }

        if (validItems.isEmpty()) {
            cheapestSlot = null;
            bestValueSlot = null;
            return;
        }

        cheapestSlot = validItems.stream()
                .min(Comparator.comparingInt(d -> d.totalPrice))
                .map(d -> d.slot)
                .orElse(null);

        bestValueSlot = validItems.stream()
                .min(Comparator.comparingInt(d -> d.pricePerItem))
                .map(d -> d.slot)
                .orElse(null);
    }

    private boolean hasMending(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        var enchantments = stack.getEnchantments();
        for (var entry : enchantments.getEnchantments()) {
            if (entry.getKey().isPresent()) {
                String enchantmentId = entry.getKey().get().getValue().toString();
                if (enchantmentId.equals("minecraft:mending")) {
                    return true;
                }
            }
        }
        return false;
    }

    private int parsePriceFromLore(ItemStack stack) {
        LoreComponent loreComp = stack.get(DataComponentTypes.LORE);
        if (loreComp == null) return 0;

        for (Text text : loreComp.lines()) {
            String line = Formatting.strip(text.getString());
            if (line == null) continue;
            Matcher m = PRICE_PATTERN.matcher(line);
            if (m.find()) {
                try {
                    String priceStr = m.group(1).replaceAll("[,\\s\\.]", "");
                    return Integer.parseInt(priceStr);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private int getBlinkingColor(int color, long time, int periodMs) {
        float alpha = (float) (Math.sin((double) time / periodMs * Math.PI) * 0.3f + 0.7f);
        float factor = Math.min(1f, Math.max(0.4f, alpha));
        int a = (int) ((color >> 24 & 0xFF) * factor);
        int r = (int) ((color >> 16 & 0xFF) * factor);
        int g = (int) ((color >> 8 & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void highlightSlot(DrawContext context, Slot slot, int color) {
        if (slot != null) {
            RenderUtil.RECT.draw(context.getMatrices(), slot.x, slot.y, 16, 16, 0, new Color(color, true));
        }
    }

    private static class ItemPriceData {
        Slot slot;
        int totalPrice;
        int pricePerItem;
        int count;

        ItemPriceData(Slot slot, int totalPrice, int pricePerItem, int count) {
            this.slot = slot;
            this.totalPrice = totalPrice;
            this.pricePerItem = pricePerItem;
            this.count = count;
        }
    }
}

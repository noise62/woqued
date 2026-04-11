package sweetie.evaware.api.utils.auction;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import sweetie.evaware.api.system.interfaces.QuickImports;

public class PriceParser implements QuickImports {
    public ParseModeChoice currentMode = ParseModeChoice.FUN_TIME;

    public int getPrice(ItemStack stack) {
        for (Text text : stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC)) {
            String str = text.getString().replace("§r", "").replace("¤", "").trim();
            String textPrice = getStr();
            if (str.startsWith(textPrice)) return Integer.parseInt(str.replace(textPrice, "").replace(",", "").replace(" ", "").trim());
        }
        return -1;
    }

    private String getStr() {
        switch (currentMode) {
            case FUN_TIME -> { return "$ Ценa $"; }
            case SPOOKY_TIME -> { return "$ Цена: $"; }
            case HOLY_WORLD -> { return "▍ Цена за 1 ед.:"; }
            case REALLY_WORLD -> { return "Цена:"; }
        }
        return "";
    }
}

package sweetie.evaware.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.fonts.MsdfGlyph;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TextUtil implements QuickImports {
    public java.util.List<MsdfGlyph.ColoredGlyph> parseTextToColoredGlyphs(Text text) {
        java.util.List<MsdfGlyph.ColoredGlyph> result = new ArrayList<>();
        parseTextRecursive(text, 0xFFFFFFFF, result);
        return result;
    }

    private void parseTextRecursive(Text text, int currentColor, List<MsdfGlyph.ColoredGlyph> result) {
        Style style = text.getStyle();
        int color = style.getColor() != null ? style.getColor().getRgb() | 0xFF000000 : currentColor;

        TextContent content = text.getContent();
        String raw = "";

        if (content instanceof PlainTextContent.Literal(String string)) {
            raw = string;
        } else if (content instanceof TranslatableTextContent translatable) {
            raw = translatable.getKey();
        } else if (content instanceof KeybindTextContent keybind) {
            raw = keybind.getKey();
        }

        raw = ReplaceUtil.replaceSymbols(raw);

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c == 'ย' || c == 'ง') && i + 1 < raw.length()) {
                i++;
                continue;
            }
            result.add(new MsdfGlyph.ColoredGlyph(c, color));
        }

        for (Text sibling : text.getSiblings()) {
            parseTextRecursive(sibling, color, result);
        }
    }

    public void sendMessage(String message) {
        mc.player.sendMessage(Text.literal("").append(gradient(ClientInfo.NAME, true)).append(Text.literal(Formatting.GRAY + " >> " + Formatting.RESET + message)), false);
    }

    public String getDurationText(int ticks) {
        if (ticks == -1) {
            return "**:**";
        }

        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        minutes %= 60;
        int remainingSeconds = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, remainingSeconds);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, remainingSeconds);
        }
        return String.format("%ds", seconds);
    }

    public MutableText gradient(String message, boolean bold) {
        MutableText text = Text.empty();
        int length = message.length();
        for (int i = 0; i < length; i++) {
            Color color = ColorUtil.gradient((float) i / (length - 1), UIColors.primary(), UIColors.secondary());
            text.append(Text.literal(String.valueOf(message.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getRGB())).withBold(bold)));
        }
        return text;
    }
}

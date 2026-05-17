package worst.woqued.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.client.ui.widget.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ScoreboardWidget extends Widget {

    public ScoreboardWidget() {
        super(3f, 50f);
    }

    @Override
    public String getName() {
        return "Scoreboard";
    }

    @Override
    public void render(MatrixStack m) {
        if (mc.player == null || mc.world == null) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        ScoreboardObjective objective = mc.world.getScoreboard()
                .getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (objective == null) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float titleSize = scaled(6f);
        float lineSize  = scaled(5f);
        float padX      = scaled(6f);
        float padY      = scaled(3f);
        float lineGap   = scaled(3f);
        float valueGap  = scaled(8f);
        float round     = scaled(5f);

        List<ScoreboardEntry> entries = mc.world.getScoreboard()
                .getScoreboardEntries(objective).stream()
                .filter(e -> e != null && !e.hidden())
                .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed()
                        .thenComparing(ScoreboardEntry::owner, String::compareToIgnoreCase))
                .limit(15)
                .toList();

        Text titleText = objective.getDisplayName();
        String titlePlain = net.minecraft.util.Formatting.strip(titleText.getString());
        float titleW   = getSemiBoldFont().getWidth(titlePlain != null ? titlePlain : titleText.getString(), titleSize);
        float maxWidth = titleW;

        record EntryData(Text name, String namePlain, float nameW,
                         Text value, String valuePlain, float valueW) {}
        List<EntryData> entryData = new ArrayList<>();

        for (ScoreboardEntry entry : entries) {
            Text nameText  = resolveNameText(objective.getScoreboard(), entry);
            Text valueText = resolveValueText(objective, entry);
            String namePlain  = strip(nameText.getString());
            String valuePlain = strip(valueText.getString());
            float nw = getMediumFont().getWidth(namePlain,  lineSize);
            float vw = getMediumFont().getWidth(valuePlain, lineSize);
            maxWidth = Math.max(maxWidth, nw + vw + valueGap);
            entryData.add(new EntryData(nameText, namePlain, nw, valueText, valuePlain, vw));
        }

        float headerH = titleSize + padY * 2f;
        float lineH   = lineSize + lineGap;
        float totalH  = headerH + entryData.size() * lineH + padY;
        float totalW  = maxWidth + padX * 2f + scaled(8f);

        RenderUtil.RECT.draw(m, x, y, totalW, totalH, round, UIColors.widgetBlur());
        RenderUtil.BLUR_RECT.draw(m, x, y, totalW, totalH, round, UIColors.widgetBlur());

        float sepH = scaled(1f);
        RenderUtil.GRADIENT_RECT.draw(m, x + padX, y + headerH - sepH,
                totalW - padX * 2f, sepH, 0f,
                UIColors.primary(), UIColors.secondary(), UIColors.primary(), UIColors.secondary());

        float titleX = x + (totalW - titleW) / 2f;
        drawColoredText(m, titleText, titleX, y + padY, titleSize, true);

        float lineY = y + headerH;
        for (EntryData ed : entryData) {
            float rowY = lineY + (lineH - lineSize) / 2f;

            drawColoredText(m, ed.name(), x + padX, rowY, lineSize, false);
            drawColoredText(m, ed.value(), x + totalW - padX - ed.valueW(), rowY, lineSize, false);

            lineY += lineH;
        }

        getDraggable().setWidth(totalW);
        getDraggable().setHeight(totalH);
    }

    private Text resolveNameText(Scoreboard scoreboard, ScoreboardEntry entry) {
        Text display = entry.display();
        if (display != null) return display;
        String owner = entry.owner();
        if (owner == null) return Text.empty();
        Team team = scoreboard.getScoreHolderTeam(owner);
        Text base = Text.literal(owner);
        return team != null ? Team.decorateName(team, base) : base;
    }

    private Text resolveValueText(ScoreboardObjective objective, ScoreboardEntry entry) {
        Text formatted = entry.formatted(objective.getNumberFormatOr(StyledNumberFormat.EMPTY));
        return formatted != null ? formatted : Text.literal(Integer.toString(entry.value()));
    }

    private String strip(String s) {
        if (s == null) return "";
        String stripped = net.minecraft.util.Formatting.strip(s);
        return stripped != null ? stripped : s;
    }

    private void drawColoredText(MatrixStack m, Text text, float x, float y,
                                  float size, boolean bold) {
        if (text == null) return;

        List<String[]> textSegs = new ArrayList<>();

        int defaultColor = UIColors.textColor().getRGB();

        text.visit((style, str) -> {
            String plain = net.minecraft.util.Formatting.strip(str);
            if (plain == null || plain.isEmpty()) return Optional.empty();
            int color = style.getColor() != null
                    ? (0xFF000000 | (style.getColor().getRgb() & 0xFFFFFF))
                    : defaultColor;
            textSegs.add(new String[]{plain, String.valueOf(color)});
            return Optional.empty();
        }, net.minecraft.text.Style.EMPTY);

        float curX = x;
        for (String[] seg : textSegs) {
            String txt = seg[0];
            int color = Integer.parseInt(seg[1]);
            Color c = new Color(color, true);
            if (bold) {
                getSemiBoldFont().drawText(m, txt, curX, y, size, c);
                curX += getSemiBoldFont().getWidth(txt, size);
            } else {
                getMediumFont().drawText(m, txt, curX, y, size, c);
                curX += getMediumFont().getWidth(txt, size);
            }
        }
    }
}
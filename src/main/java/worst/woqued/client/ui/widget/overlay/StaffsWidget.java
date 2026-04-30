package worst.woqued.client.ui.widget.overlay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import worst.woqued.api.system.configs.StaffManager;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.framelimiter.FrameLimiter;
import worst.woqued.api.utils.other.ReplaceUtil;
import worst.woqued.api.utils.player.PlayerUtil;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;
import java.util.List;

public class StaffsWidget extends ContainerWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();
    private final Map<String, Float> animMap = new HashMap<>();

    public record Staff(String name, Status status) {}

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        ONLINE("Online"),
        NEAR("Near"),
        GM3("Gm3"),
        VANISH("Vanish");

        private final String label;
    }

    public StaffsWidget() {
        super(100f, 100f);
    }

    @Override
    public String getName() {
        return "Staff Online";
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        return null;
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        List<Staff> currentStaffs = getStaffList();
        Set<String> activeNames = new HashSet<>();
        Map<String, Staff> staffData = new HashMap<>();

        for (Staff s : currentStaffs) {
            activeNames.add(s.name());
            staffData.put(s.name(), s);
        }

        activeNames.forEach(name -> {
            float currentAnim = animMap.getOrDefault(name, 0f);
            animMap.put(name, currentAnim + (1f - currentAnim) * 0.15f);
        });

        animMap.keySet().forEach(name -> {
            if (!activeNames.contains(name)) {
                float currentAnim = animMap.get(name);
                animMap.put(name, currentAnim + (0f - currentAnim) * 0.15f);
            }
        });

        animMap.entrySet().removeIf(e -> e.getValue() < 0.01f && !activeNames.contains(e.getKey()));

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(12f);
        float p = scaled(4.5f);
        float fontSize = scaled(6f);
        float round = h * 0.25f;
        float iconPadding = scaled(5f);

        String title = getName();
        String icon = "s"; // Иконка для стаффа

        // Расчет максимальной ширины
        float titleWidth = getMediumFont().getWidth(title, fontSize);
        float iconWidth = Fonts.WOQUED.getWidth(icon, fontSize);
        float totalTitleContentWidth = titleWidth + iconPadding + iconWidth;

        float maxW = totalTitleContentWidth + p * 2f;

        for (String name : animMap.keySet()) {
            if (animMap.get(name) < 0.05f) continue;
            Staff staff = staffData.get(name);
            if (staff == null) continue;

            float nameW = getMediumFont().getWidth(staff.name(), fontSize);
            float labelW = getMediumFont().getWidth(staff.status().getLabel(), fontSize);
            float totalRowW = nameW + labelW + p * 4f;
            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        // Рендер заголовка
        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());

        float centerY = currentY + h / 2f - fontSize / 2f;
        float currentX = renderX + p;

        getMediumFont().drawText(ms, title, currentX, centerY, fontSize, UIColors.textColor());

        float iconX = renderX + maxW - p - iconWidth;
        Fonts.WOQUED.drawGradientText(ms, icon, iconX, centerY,
                fontSize, UIColors.primary(), UIColors.secondary(), maxW / 4f);

        currentY += h + 2.5f;

        // Рендер списка
        for (String name : animMap.keySet()) {
            float anim = animMap.get(name);
            if (anim <= 0.01f) continue;

            Staff staff = staffData.get(name);
            if (staff == null) continue;

            float rowH = h * anim;
            int alpha = (int) (255 * anim);

            float textCenterY = currentY + (rowH / 2f) - (fontSize / 2f);

            if (anim > 0.05f) {
                Color textColor = UIColors.textColor();
                Color dynamicText = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha);

                // Цвет для статуса
                Color statusColor = switch (staff.status()) {
                    case ONLINE -> UIColors.positiveColor();
                    case NEAR -> UIColors.middleColor();
                    case GM3, VANISH -> UIColors.negativeColor();
                };
                Color statusTextColor = new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), alpha);
                Color statusBoxColor = new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), (int)(255 * anim));

                // Рект для имени
                float nameTextW = getMediumFont().getWidth(staff.name(), fontSize);
                float nameRectW = nameTextW + scaled(9);
                float nameRectH = (fontSize + scaled(6f)) * anim;
                float nameRectX = renderX;
                float nameRectY = currentY + (rowH / 2f) - (nameRectH / 2f);

                RenderUtil.RECT.draw(ms, nameRectX, nameRectY, nameRectW, nameRectH, 2.5f, UIColors.widgetBlur());
                getMediumFont().drawText(ms, staff.name(),
                        nameRectX + (nameRectW / 2f) - (nameTextW / 2f),
                        textCenterY, fontSize, dynamicText);

                // Рект для статуса
                String statusLabel = staff.status().getLabel();
                float statusTextW = getMediumFont().getWidth(statusLabel, fontSize);
                float statusRectW = statusTextW + scaled(9);
                float statusRectH = (fontSize + scaled(6f)) * anim;
                float gapAfterName = scaled(2f);
                float statusRectX = nameRectX + nameRectW + gapAfterName;
                float statusRectY = currentY + (rowH / 2f) - (statusRectH / 2f);

                RenderUtil.RECT.draw(ms, statusRectX, statusRectY, statusRectW, statusRectH, 2.5f, statusBoxColor);
                getMediumFont().drawText(ms, statusLabel,
                        statusRectX + (statusRectW / 2f) - (statusTextW / 2f),
                        textCenterY, fontSize, statusTextColor);
            }

            currentY += rowH + 1.5f;
        }

        getDraggable().setWidth(maxW);
        getDraggable().setHeight(currentY - y);
    }

    private List<Staff> getStaffList() {
        frameLimiter.execute(15, () -> {
            List<Staff> list = new ArrayList<>();
            if (!mc.isInSingleplayer()) {
                list.addAll(getOnlineStaff());
                list.addAll(getVanishedPlayers());
            }
            cacheStaffs = list;
        });
        return cacheStaffs;
    }

    private List<Staff> getOnlineStaff() {
        List<Staff> staff = new ArrayList<>();
        if (mc.player == null || mc.player.networkHandler == null || mc.world == null) return staff;

        for (PlayerListEntry player : mc.player.networkHandler.getPlayerList()) {
            Team team = player.getScoreboardTeam();
            if (team == null) continue;

            String name = player.getProfile().getName();
            if (!PlayerUtil.isValidName(name)) continue;

            String prefix = ReplaceUtil.replaceSymbols(team.getPrefix().getString());

            if (StaffManager.getInstance().contains(name) || isStaffPrefix(prefix.toLowerCase())) {
                Status status = Status.ONLINE;

                if (player.getGameMode() == GameMode.SPECTATOR) {
                    status = Status.GM3;
                } else if (mc.world.getPlayers().stream().anyMatch(p -> p.getGameProfile().getName().equals(name))) {
                    status = Status.NEAR;
                }

                staff.add(new Staff(prefix + " " + name, status));
            }
        }
        return staff;
    }

    private List<Staff> getVanishedPlayers() {
        List<Staff> vanished = new ArrayList<>();
        if (mc.world == null || mc.world.getScoreboard() == null || mc.getNetworkHandler() == null)
            return vanished;

        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            onlineNames.add(entry.getProfile().getName());
        }

        for (Team team : mc.world.getScoreboard().getTeams()) {
            for (String name : team.getPlayerList()) {
                if (!PlayerUtil.isValidName(name)) continue;
                if (!onlineNames.contains(name)) {
                    vanished.add(new Staff(name, Status.VANISH));
                }
            }
        }

        return vanished;
    }

    private boolean isStaffPrefix(String prefix) {
        return (prefix.contains("helper") || prefix.contains("moder") || prefix.contains("admin") ||
                prefix.contains("owner") || prefix.contains("developer") || prefix.contains("staff") ||
                prefix.contains("curator") || prefix.contains("куратор") || prefix.contains("разраб") ||
                prefix.contains("модер") || prefix.contains("админ") || prefix.contains("стажер") ||
                prefix.contains("стажёр") || prefix.contains("хелпер"));
    }
}
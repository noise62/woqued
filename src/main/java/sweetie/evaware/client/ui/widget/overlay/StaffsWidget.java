package sweetie.evaware.client.ui.widget.overlay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import sweetie.evaware.api.system.configs.StaffManager;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.framelimiter.FrameLimiter;
import sweetie.evaware.api.utils.other.ReplaceUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.ContainerWidget; // Вот этот важный импорт

import java.awt.*;
import java.util.*;
import java.util.List;

public class StaffsWidget extends ContainerWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();

    // Карта анимаций для плавного появления/скрытия строк
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
        return "Staffs";
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        return null; // Не используется, так как переопределен render
    }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        // 1. Получаем текущий список персонала
        List<Staff> currentStaffs = getStaffList();
        Set<String> activeNames = new HashSet<>();
        Map<String, Staff> staffData = new HashMap<>();

        for (Staff s : currentStaffs) {
            activeNames.add(s.name());
            staffData.put(s.name(), s);
        }

        // 2. Обновление анимаций (как в CooldownsWidget)
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

        // 3. Геометрия
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(14f);
        float p = scaled(4.5f);
        float fontSize = scaled(7f);
        float round = h * 0.25f;

        String title = getName();

        // 4. Расчет максимальной ширины
        float maxW = getMediumFont().getWidth(title, fontSize) + p * 6f;
        for (String name : animMap.keySet()) {
            if (animMap.get(name) < 0.05f) continue;

            Staff staff = staffData.get(name);
            String label = (staff != null) ? staff.status().getLabel() : "";

            // Имя + отступ + ширина плашки статуса
            float nameW = getMediumFont().getWidth(name, fontSize);
            float labelW = getMediumFont().getWidth(label, fontSize) + scaled(9);
            float totalRowW = nameW + labelW + p * 8f;

            if (totalRowW > maxW) maxW = totalRowW;
        }

        float renderX = isRightSide ? (x + width - maxW) : x;
        float currentY = y;

        // 5. Рендер заголовка
        RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, h, round, UIColors.widgetBlur());
        float titleWidth = getMediumFont().getWidth(title, fontSize);
        getMediumFont().drawGradientText(ms, title,
                renderX + (maxW / 2f) - (titleWidth / 2f),
                currentY + h / 2f - fontSize / 2f,
                fontSize, UIColors.primary(), UIColors.secondary(), maxW / 4f);

        currentY += h + 2.5f;

        // 6. Рендер списка (анимированно)
        for (String name : animMap.keySet()) {
            float anim = animMap.get(name);
            if (anim <= 0.01f) continue;

            Staff staff = staffData.get(name);
            if (staff == null) continue;

            float rowH = h * anim;
            int alpha = (int) (255 * anim);

            // Фон строки
            Color themeBlur = UIColors.widgetBlur();
            Color dynamicBg = new Color(themeBlur.getRed(), themeBlur.getGreen(), themeBlur.getBlue(), (int)(themeBlur.getAlpha() * anim));
            RenderUtil.BLUR_RECT.draw(ms, renderX, currentY, maxW, rowH, round, dynamicBg);

            if (anim > 0.05f) {
                Color textColor = UIColors.textColor();
                Color dynamicText = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha);
                float textCenterY = currentY + (rowH / 2f) - (fontSize / 2f);

                // Имя сотрудника
                getMediumFont().drawText(ms, staff.name(), renderX + p + 2f, textCenterY, fontSize, dynamicText);

                // Цвет статуса (как в оригинале)
                Color statusColor = switch (staff.status()) {
                    case ONLINE -> UIColors.positiveColor();
                    case NEAR -> UIColors.middleColor();
                    case GM3, VANISH -> UIColors.negativeColor();
                };

                // Плашка статуса (как бокс со временем в кулдаунах)
                String statusLabel = staff.status().getLabel();
                float statTextW = getMediumFont().getWidth(statusLabel, fontSize);
                float boxW = statTextW + scaled(9);
                float boxH = (fontSize + scaled(2.5f)) * anim;
                float boxX = renderX + maxW - p - boxW;
                float boxY = currentY + (rowH / 2f) - (boxH / 2f);

                // Фон плашки статуса (полупрозрачный цвет статуса)
                Color statusBoxColor = new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), (int)(40 * anim));
                Color statusTextColor = new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), alpha);

                RenderUtil.RECT.draw(ms, boxX, boxY, boxW, boxH, 2.5f, statusBoxColor);
                getMediumFont().drawText(ms, statusLabel, boxX + (boxW / 2f) - (statTextW / 2f), textCenterY, fontSize, statusTextColor);
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
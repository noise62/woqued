import sweetie.evaware.api.system.configs.ThemeManager;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.client.ui.theme.Theme;

import java.awt.*;

public class ThemeColorsParser {
    public static final ThemeManager themeManager = new ThemeManager("C:\\Users\\David Lynch\\IdeaProjects\\EvaWare\\run\\EvaWare\\themes");

    public static void main(String[] args) {
        parseWithThemeManager("Pink");
    }

    public static void parseWithThemeManager(String themeName) {
        Theme theme = themeManager.load(themeName);

        if (theme == null) {
            System.out.println("Тема '" + themeName + "' не найдена");
            return;
        }

        System.out.println("Тема: " + theme.getName());
        System.out.println("Цвета:");

        theme.getElementColors().forEach(element -> {
            Color color = element.getColor();
            int rgb = color.getRGB();
            int[] unpacked = ColorUtil.unpack(rgb);

            System.out.printf("  %-20s | rgb(%3d, %3d, %3d, %3d) | #%08X%n",
                    element.getName(),
                    unpacked[0], unpacked[1], unpacked[2], unpacked[3],
                    rgb & 0xFFFFFFFFL);
        });
    }
}
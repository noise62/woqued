package worst.woqued.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.fonts.Font;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.ui.widget.Widget;

import java.awt.Color;

public class WatermarkWidget extends Widget {
    private float animatedFps = 0;

    public WatermarkWidget() {
        super(4f, 4f);
    }

    @Override
    public String getName() {
        return "Watermark";
    }

    @Override
    public void render(MatrixStack matrixStack) {
        if (mc.player == null) return;

        // Базовые координаты и размеры
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float h = scaled(12f);
        float p = scaled(4.5f); // Отступы по краям (padding)
        float fontSize = scaled(6f);
        float gap = scaled(3f); // Расстояние между элементами
        float rectGap = scaled(3f); // Расстояние между двумя плашками
        float userGap = scaled(4f); // Отступ между названием клиента и именем пользователя

        Font font = getMediumFont();
        Font woquedFont = Fonts.WOQUED; // Шрифт WOQUED для иконок
        Font woquedlogoFont = Fonts.LOGO; // Шрифт WOQUED для иконок

        // Сбор информации
        animatedFps = MathHelper.lerp(0.1f, animatedFps, mc.getCurrentFps());
        String fpsText = "FPS: " + Math.round(animatedFps);
        String ipText = (mc.getCurrentServerEntry() != null) ? mc.getCurrentServerEntry().address : "singleplayer";
        String pcName = System.getProperty("user.name");

        String clientName = "t.me/woqued";

        // Иконки для каждого элемента (шрифт WOQUED)
        String logoIcon = "B"; // Иконка для логотипа
        String userIcon = "n"; // Иконка пользователя
        String serverIcon = "o"; // Иконка сервера
        String fpsIcon = "p"; // Иконка FPS

        // Вычисление ширины текста
        float logoIconWidth = woquedFont.getWidth(logoIcon, fontSize * 1.2f);
        float logoNameWidth = font.getWidth(clientName, fontSize);
        float userIconWidth = woquedFont.getWidth(userIcon, fontSize);
        float userNameWidth = font.getWidth(pcName, fontSize);
        float serverIconWidth = woquedFont.getWidth(serverIcon, fontSize);
        float serverIpWidth = font.getWidth(ipText, fontSize);
        float fpsIconWidth = woquedFont.getWidth(fpsIcon, fontSize);
        float fpsTextWidth = font.getWidth(fpsText, fontSize);

        float separatorWidth = scaled(1f); // Ширина разделителя

        // Общая ширина плашки
        float totalWidth = p + logoIconWidth + gap + logoNameWidth + gap +
                separatorWidth + gap +
                userIconWidth + gap + userNameWidth + gap +
                separatorWidth + gap +
                serverIconWidth + gap + serverIpWidth + gap +
                separatorWidth + gap +
                fpsIconWidth + gap + fpsTextWidth + p;

        float rectX = x;

        // Цвета
        Color bgColor = UIColors.widgetBlur();
        Color textColor = UIColors.textColor();
        Color separatorColor = new Color(128, 128, 128, 100); // Серый полупрозрачный
        float round = h * 0.25f;

        // Отрисовка фона
        RenderUtil.BLUR_RECT.draw(matrixStack, rectX, y, totalWidth - 15, h, round, bgColor);

        // Центрирование по высоте
        float textY = y + (h / 2f) - (fontSize / 2f);
        float iconY = y + (h / 2f) - ((fontSize * 1.2f) / 2f);

        float currentX = rectX + p;

        // ===== ЛОГОТИП: иконка + Woqued =====
        // Иконка логотипа (WOQUED шрифт)
        woquedlogoFont.drawGradientText(matrixStack, logoIcon, currentX, iconY, fontSize * 1.1f,
                UIColors.primary(), UIColors.secondary(), logoIconWidth / 4f);
        currentX += logoIconWidth + gap;

        // Текст Woqued
        font.drawGradientText(matrixStack, clientName, currentX, textY, fontSize,
                UIColors.primary(), UIColors.secondary(), logoNameWidth / 4f);
        currentX += logoNameWidth + gap;

        // Разделитель 1
//        RenderUtil.BLUR_RECT.draw(matrixStack, currentX, y + scaled(2f), separatorWidth, h - scaled(4f), scaled(1f), separatorColor);
//        currentX += separatorWidth + gap;

        // ===== ПОЛЬЗОВАТЕЛЬ =====
        // Иконка пользователя
        woquedFont.drawGradientText(matrixStack, userIcon, currentX + 1, iconY, fontSize * 1.1f, UIColors.primary(), UIColors.secondary(), logoIconWidth / 4f);
        currentX += userIconWidth + gap;

        // Имя пользователя
        font.drawText(matrixStack, pcName, currentX, textY, fontSize, textColor);
        currentX += userNameWidth + gap;

        // Разделитель 2
//        RenderUtil.BLUR_RECT.draw(matrixStack, currentX, y + scaled(2f), separatorWidth, h - scaled(4f), scaled(1f), separatorColor);
//        currentX += separatorWidth + gap;

        // ===== СЕРВЕР =====
        // Иконка сервера
        woquedFont.drawGradientText(matrixStack, serverIcon, currentX, iconY, fontSize * 1.1f, UIColors.primary(), UIColors.secondary(), logoIconWidth / 4f);
        currentX += serverIconWidth + gap;

        // IP сервера
        font.drawText(matrixStack, ipText, currentX, textY, fontSize, textColor);
        currentX += serverIpWidth + gap;

        // Разделитель 3
//        RenderUtil.BLUR_RECT.draw(matrixStack, currentX, y + scaled(2f), separatorWidth, h - scaled(4f), scaled(1f), separatorColor);
//        currentX += separatorWidth + gap;

        // ===== FPS =====
        // Иконка FPS
        woquedFont.drawGradientText(matrixStack, fpsIcon, currentX, iconY, fontSize * 1.2f,  UIColors.primary(), UIColors.secondary(), logoIconWidth / 4f);
        currentX += fpsIconWidth + gap;

        // Текст FPS
        font.drawText(matrixStack, fpsText, currentX, textY, fontSize, textColor);

        // Обновление зоны перетаскивания
        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(h);
    }
}
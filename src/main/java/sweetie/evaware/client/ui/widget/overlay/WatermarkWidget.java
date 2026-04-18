package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Font;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.widget.Widget;

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

        float h = scaled(14f);
        float p = scaled(4.5f); // Отступы по краям (padding)
        float fontSize = scaled(7f);
        float gap = scaled(3f); // Расстояние между буквой B и Woqued
        float rectGap = scaled(3f); // Расстояние между двумя плашками
        float userGap = scaled(4f); // Отступ между названием клиента и именем пользователя

        Font font = getMediumFont();
        Font logoFont = Fonts.LOGO; // Шрифт LOGO

        // Сбор информации
        animatedFps = MathHelper.lerp(0.1f, animatedFps, mc.getCurrentFps());
        String fpsText = "FPS: " + Math.round(animatedFps);
        String ipText = (mc.getCurrentServerEntry() != null) ? mc.getCurrentServerEntry().address : "singleplayer";
        String pcName = System.getProperty("user.name");

        String clientName = "Woqued";
        String logoLetter = "B"; // Теперь B!

        // Вычисление ширины текста
        float logoWidth = logoFont.getWidth(logoLetter, fontSize * 1.4f);
        float nameWidth = font.getWidth(clientName, fontSize);
        float userWidth = font.getWidth(pcName, fontSize);
        float ipWidth = font.getWidth(ipText, fontSize);
        float fpsWidth = font.getWidth(fpsText, fontSize);

        // Общая ширина одной плашки
        float totalWidth = p + logoWidth + gap + nameWidth + gap + userWidth + gap + ipWidth + gap + fpsWidth + p;

        float rectX = x;

        // Цвета
        Color bgColor = UIColors.widgetBlur();
        Color textColor = UIColors.textColor();
        float round = h * 0.25f;

        // Отрисовка фона
        RenderUtil.BLUR_RECT.draw(matrixStack, rectX, y, totalWidth, h, round, bgColor);

        // Центрирование по высоте
        float textY = y + (h / 2f) - (fontSize / 2f);

        // Рисуем букву B (логотип) с градиентом
        float currentX = rectX + p;
        float logoY = y + (h / 2f) - ((fontSize * 1.4f) / 2f);

        logoFont.drawGradientText(matrixStack, logoLetter, currentX, logoY, fontSize * 1.4f,
                UIColors.primary(), UIColors.secondary(), logoWidth / 4f);

        // Затем Woqued
        currentX += logoWidth + gap;
        font.drawGradientText(matrixStack, clientName, currentX, textY, fontSize,
                UIColors.primary(), UIColors.secondary(), nameWidth / 4f);

        // Отрисовка информации (User -> IP -> FPS)
        currentX += nameWidth + userGap;

        // Имя пользователя
        font.drawText(matrixStack, pcName, currentX, textY, fontSize, textColor);
        currentX += userWidth + gap;

        // IP сервера
        font.drawText(matrixStack, ipText, currentX, textY, fontSize, textColor);
        currentX += ipWidth + gap;

        // FPS
        font.drawText(matrixStack, fpsText, currentX, textY, fontSize, textColor);

        // Обновление зоны перетаскивания
        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(h);
    }
}
package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Font;
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
        float gap = scaled(5f); // Расстояние между словами (user, ip, fps)
        float rectGap = scaled(3f); // Расстояние между двумя плашками

        Font font = getMediumFont();

        // Сбор информации
        animatedFps = MathHelper.lerp(0.1f, animatedFps, mc.getCurrentFps());
        String fpsText = "FPS: " + Math.round(animatedFps);
        String ipText = (mc.getCurrentServerEntry() != null) ? mc.getCurrentServerEntry().address : "singleplayer";
        String pcName = System.getProperty("user.name");

        // ======= ВОТ ЗДЕСЬ МЫ ИЗМЕНИЛИ НАЗВАНИЕ =======
        String clientName = "Woqued"; // Можешь написать тут любой текст!
        // ==============================================

        // Вычисление ширины текста
        float nameWidth = font.getWidth(clientName, fontSize);
        float userWidth = font.getWidth(pcName, fontSize);
        float ipWidth = font.getWidth(ipText, fontSize);
        float fpsWidth = font.getWidth(fpsText, fontSize);

        // Вычисление ширины плашек
        float wNameRect = p + nameWidth + p;
        float wInfoRect = p + userWidth + gap + ipWidth + gap + fpsWidth + p;

        float firstRectX = x;
        float secondRectX = firstRectX + wNameRect + rectGap;

        // Цвета
        Color bgColor = UIColors.widgetBlur(); // Можно заменить на new Color(12, 12, 18, 240) для точной копии цвета Minced
        Color textColor = UIColors.textColor();
        float round = h * 0.25f; // Скругление

        // Отрисовка фонов (используем стандартный блюр Evaware для красивого вида)
        RenderUtil.BLUR_RECT.draw(matrixStack, firstRectX, y, wNameRect, h, round, bgColor);
        RenderUtil.BLUR_RECT.draw(matrixStack, secondRectX, y, wInfoRect, h, round, bgColor);

        // Центрирование текста по высоте
        float textY = y + (h / 2f) - (fontSize / 2f);

        // 1. Отрисовка названия клиента (с градиентом)
        float currentTextX = firstRectX + p;
        font.drawGradientText(matrixStack, clientName, currentTextX, textY, fontSize, UIColors.primary(), UIColors.secondary(), nameWidth / 4f);

        // 2. Отрисовка информации (User -> IP -> FPS)
        currentTextX = secondRectX + p;

        // Имя пользователя
        font.drawText(matrixStack, pcName, currentTextX, textY, fontSize, textColor);
        currentTextX += userWidth + gap;

        // IP сервера
        font.drawText(matrixStack, ipText, currentTextX, textY, fontSize, textColor);
        currentTextX += ipWidth + gap;

        // FPS
        font.drawText(matrixStack, fpsText, currentTextX, textY, fontSize, textColor);

        // Обновление зоны перетаскивания (Draggable)
        getDraggable().setWidth(wNameRect + rectGap + wInfoRect);
        getDraggable().setHeight(h);
    }
}
package sweetie.evaware.api.utils.color;

import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.utils.math.MathUtil;

import java.awt.Color;

@UtilityClass
public class ColorUtil {
    public float[] normalize(Color color) {
        return new float[] {color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f};
    }

    public float[] normalize(int color) {
        int[] components = unpack(color);
        return new float[] {components[0] / 255.0f, components[1] / 255.0f, components[2] / 255.0f, components[3] / 255.0f};
    }

    public int[] unpack(int color) {
        return new int[] {color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF};
    }

    public Color setAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
    }

    public Color gradient(int speed, int index, Color... colors) {
        int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
        angle = ((angle > 180) ? (360 - angle) : angle) + 180;
        int colorIndex = (int)(angle / 360.0f * colors.length);

        if (colorIndex == colors.length) {
            colorIndex--;
        }

        Color color1 = colors[colorIndex];
        Color color2 = colors[(colorIndex == colors.length - 1) ? 0 : (colorIndex + 1)];

        return interpolate(color1, color2, angle / 360.0f * colors.length - colorIndex);
    }

    public Color interpolate(Color to, Color from, double amount) {
        return interpolate(to, from, (float) amount);
    }

    public Color interpolate(Color to, Color from, float amount) {
        float clampedAmount = 1f - MathHelper.clamp(amount, 0f, 1f);

        int red1 = to.getRed();
        int green1 = to.getGreen();
        int blue1 = to.getBlue();
        int alpha1 = to.getAlpha();

        int red2 = from.getRed();
        int green2 = from.getGreen();
        int blue2 = from.getBlue();
        int alpha2 = from.getAlpha();

        int interpolatedRed = MathUtil.interpolate(red1, red2, clampedAmount);
        int interpolatedGreen = MathUtil.interpolate(green1, green2, clampedAmount);
        int interpolatedBlue = MathUtil.interpolate(blue1, blue2, clampedAmount);
        int interpolatedAlpha = MathUtil.interpolate(alpha1, alpha2, clampedAmount);

        return new Color(interpolatedRed, interpolatedGreen, interpolatedBlue, interpolatedAlpha);
    }

    public static Color gradient(float progress, Color color1, Color color2) {
        if (progress < 0 || progress > 1) {
            progress = progress % 1;
            if (progress < 0) progress += 1;
        }

        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * progress);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * progress);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * progress);

        return new Color(r, g, b);
    }

    public int gradient(int color1, int color2, float position, float totalWidth, float time, float offset) {
        float gradientLength = 18.0f / offset;
        float wavePosition = (time + position / (totalWidth * gradientLength)) % 1.0f;
        float factor = (float) Math.sin(wavePosition * Math.PI * 2) * 0.5f + 0.5f;

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Color flashingColor(Color flashColor, Color defaultColor) {
        return flashingColor(flashColor, defaultColor, 1f);
    }

    public Color flashingColor(Color flashColor, Color defaultColor, float alpha) {
        double time = System.currentTimeMillis() % 1000 / 1000.0;
        float flashFactor = (float) (Math.sin(time * Math.PI * 2) * 0.5 + 0.5);
        return flashingColor(flashColor, defaultColor, alpha, flashFactor);
    }

    public Color flashingColor(Color flashColor, Color defaultColor, float alpha, float flashFactor) {
        float[] def = ColorUtil.normalize(defaultColor);
        float[] flash = ColorUtil.normalize(flashColor);
        return new Color((int) ((flash[0] * flashFactor + def[0] * (1 - flashFactor)) * 255),
                (int) ((flash[1] * flashFactor + def[1] * (1 - flashFactor)) * 255),
                (int) ((flash[2] * flashFactor + def[2] * (1 - flashFactor)) * 255),
                (int) (alpha * 255)
        );
    }
}

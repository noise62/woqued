package sweetie.evaware.api.utils.render.fonts;

import java.awt.Color;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.backend.Pair;
import sweetie.evaware.api.system.files.FileUtil;
import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.other.ReplaceUtil;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.api.utils.render.ScissorUtil;
import sweetie.evaware.api.utils.render.fonts.FontData.AtlasData;
import sweetie.evaware.api.utils.render.fonts.FontData.GlyphData;
import sweetie.evaware.api.utils.render.fonts.FontData.MetricsData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import sweetie.evaware.client.services.RenderService;

public final class Font {
    private static final ShaderProgramKey shaderKey = new ShaderProgramKey(FileUtil.getShader("text"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private final String name;
    private final AbstractTexture texture;
    @Getter private final AtlasData atlas;
    @Getter private final MetricsData metrics;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Integer, Map<Integer, Float>> kernings;

    public Font(String name, AbstractTexture texture, AtlasData atlas, MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
        this.name = name;
        this.texture = texture;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
    }

    private Pair<Float, Float> offset(float x, float y) {
        float scale = RenderService.getInstance().getScale();

        float x1 = x;
        float y1 = y;

        boolean isPS = name.contains(Fonts.ps);
        boolean isSF = name.contains(Fonts.sf);

        if (isSF || isPS) {
            y1 -= scale;
            if (isPS) {
                x1 -= scale / 2f;
            }
        }

        return new Pair<>(x1, y1);
    }

    public void drawText(MatrixStack matrixStack, Text text, float x, float y, float size, float thickness, float smoothness, float spacing, int outlineColor, float outlineThickness) {
        if (text == null) return;

        try {
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();

            start(outlineThickness, thickness, smoothness, outlineColor);

            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            applyGlyphs(matrix, builder, TextUtil.parseTextToColoredGlyphs(ReplaceUtil.replaceSymbols(text)), size, (thickness + outlineThickness * 0.5f) * 0.5f * size, spacing, x, y + getMetrics().baselineHeight() * size, 0f);

            end(builder);
        } catch (Exception e) {
            //System.out.println("Font(Text)#draw got error: " + e.getMessage());
        }
    }

    public void drawText(MatrixStack matrixStack, String text, float x, float y, float size, float thickness, int color, int colorSecond, float offset, float smoothness, float spacing, int outlineColor, float outlineThickness) {
        try {
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();

            start(outlineThickness, thickness, smoothness, outlineColor);

            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            applyGlyphs(matrix, builder, ReplaceUtil.protectedString(text), size, (thickness + outlineThickness * 0.5f) * 0.5f * size, spacing, x, y + getMetrics().baselineHeight() * size, 0f, color, colorSecond, offset);

            end(builder);
        } catch (Exception e) {
            //System.out.println("Font(String)#draw got error: " + e.getMessage());
        }
    }

    private void end(BufferBuilder builder) {
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void start(float outlineThickness, float thickness, float smoothness, int outlineColor) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(0, this.texture.getGlId());

        boolean outlineEnabled = (outlineThickness > 0.0f);
        ShaderProgram shader = RenderSystem.setShader(shaderKey);
        shader.getUniform("uRange").set(getAtlas().range());
        shader.getUniform("uThickness").set(thickness);
        shader.getUniform("uSmoothness").set(smoothness);
        shader.getUniform("uOutline").set(outlineEnabled ? 1 : 0);

        if (outlineEnabled) {
            shader.getUniform("uOutlineThickness").set(outlineThickness);
            float[] outlineComponents = ColorUtil.normalize(outlineColor);
            shader.getUniform("uOutlineColor").set(outlineComponents[0], outlineComponents[1], outlineComponents[2], outlineComponents[3]);
        }
    }


    // ************************************************************************************ //

    public void drawText(MatrixStack matrixStack, Text text, float x, float y, float size, float thickness) {
        Pair<Float, Float> coordinates = offset(x, y);

        drawText(matrixStack, text, coordinates.left(), coordinates.right(), size, thickness, 0.5f, 0f, -1, thickness);
    }

    public void drawText(MatrixStack matrixStack, String text, float x, float y, float size, Color color, float thickness) {
        Pair<Float, Float> coordinates = offset(x, y);

        drawText(matrixStack, text, coordinates.left(), coordinates.right(), size, thickness, color.getRGB(), -1, -1f, 0.5f, 0f, -1, thickness);
    }

    public void drawGradientText(MatrixStack matrixStack, String text, float x, float y, float size, Color colorFirst, Color colorSecond, float offset, float thickness) {
        Pair<Float, Float> coordinates = offset(x, y);

        drawText(matrixStack, text, coordinates.left(), coordinates.right(), size, thickness, colorFirst.getRGB(), colorSecond.getRGB(), offset, 0.5f, 0f, -1, thickness);
    }

    public void drawGradientText(MatrixStack matrixStack, String text, float x, float y, float size, Color color, Color colorSecond, float offset) {
        drawGradientText(matrixStack, text, x, y, size, color, colorSecond, offset, 0f);
    }

    public void drawText(MatrixStack matrixStack, Text text, float x, float y, float size) {
        drawText(matrixStack, text, x, y, size, 0f);
    }

    public void drawText(MatrixStack matrixStack, String text, float x, float y, float size, Color color) {
        drawText(matrixStack, text, x, y, size, color, 0f);
    }

    public void drawCenteredText(MatrixStack matrixStack, String text, float x, float y, float size, Color color, float thickness) {
        drawText(matrixStack, text, x - getWidth(text, size, thickness) / 2f, y, size, color, thickness);
    }

    public void drawCenteredText(MatrixStack matrixStack, String text, float x, float y, float size, Color color) {
        drawCenteredText(matrixStack, text, x, y, size, color, 0f);
    }

    public void drawCenteredGradientText(MatrixStack matrixStack, String text, float x, float y, float size, Color color, Color colorSecond, float offset, float thickness) {
        drawGradientText(matrixStack, text, x - getWidth(text, size, thickness) / 2f, y, size, color, colorSecond, offset, thickness);
    }

    public void drawCenteredGradientText(MatrixStack matrixStack, String text, float x, float y, float size, Color color, Color colorSecond, float offset) {
        drawCenteredGradientText(matrixStack, text, x, y, size, color, colorSecond, offset, 0f);
    }

    public void drawWrap(MatrixStack matrixStack, String text, float x, float y, float width, float size, Color color, float offset, Duration cycleDuration, Duration pauseDuration) {
        if (color.getAlpha() <= 0) return;

        float textWidth = getWidth(text, size);

        if (textWidth <= width) {
            drawText(matrixStack, text, x, y, size, color);
        } else {
            ScissorUtil.start(matrixStack, x, y - size / 4F, width, size * 1.5F);
            long cycleMillis = cycleDuration.toMillis();
            long pauseMillis = pauseDuration.toMillis();
            long totalCycleTime = cycleMillis + pauseMillis;

            long elapsed = System.currentTimeMillis() % totalCycleTime;

            float progress = (elapsed < cycleMillis)
                    ? (float) elapsed / cycleMillis
                    : 1.0F;

            float value = (Easing.SINE_BOTH.apply(progress) * (textWidth + offset));

            drawText(matrixStack, text, x - value, y, size, color);
            drawText(matrixStack, text, x - value + (textWidth + offset), y, size, color);
            ScissorUtil.stop(matrixStack);
        }
    }


    // ************************************************************************************ //

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color, int colorSecond, float offset) {
        int prevChar = -1;
        float startX = x;
        float totalWidth = getWidth(text, size);
        float time = (System.currentTimeMillis() % 3000) / 3000.0f;

        for (int i = 0; i < text.length(); i++) {
            int _char = text.charAt(i);
            MsdfGlyph glyph = this.glyphs.get(_char);

            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault(_char, 0.0f) * size;
            }

            int currentColor = color;
            if (offset > 1.0f) {
                currentColor = ColorUtil.gradient(color, colorSecond, x - startX, totalWidth, time, offset);
            }

            x += glyph.apply(matrix, consumer, size, x, y, z, currentColor) + thickness + spacing;
            prevChar = _char;
        }
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, List<MsdfGlyph.ColoredGlyph> glyphs, float size, float thickness, float spacing, float x, float y, float z) {
        int prevChar = -1;
        for (int i = 0; i < glyphs.size(); i++) {
            MsdfGlyph.ColoredGlyph glyphData = glyphs.get(i);
            int _char = glyphData.c();
            int color = glyphData.color();

            MsdfGlyph glyph = this.glyphs.get(_char);
            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault(_char, 0.0f) * size;
            }

            x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
            prevChar = _char;
        }
    }

    public float getHeight(float size) {
        return size;
    }

    public float getWidth(Text text, float size) {
        return getWidth(text, size, 0f);
    }

    public float getWidth(Text text, float size, float thickness) {
        if (text == null) return 0f;
        
        List<MsdfGlyph.ColoredGlyph> glyphs = TextUtil.parseTextToColoredGlyphs(text);
        int prevChar = -1;
        float width = 0.0f;

        for (int i = 0; i < glyphs.size(); i++) {
            int _char = glyphs.get(i).c();
            MsdfGlyph glyph = this.glyphs.get(_char);
            if (glyph == null)
                continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault(_char, 0.0f) * size * (1f + thickness);
            }

            width += glyph.getWidth(size);
            prevChar = _char;
        }

        return width;
    }

    public float getWidth(String text, float size) {
        return getWidth(text, size, 0f);
    }

    public float getWidth(String text, float size, float thickness) {
        int prevChar = -1;
        float width = 0.0f;

        String finalText = ReplaceUtil.protectedString(text);

        for (int i = 0; i < finalText.length(); i++) {
            int _char = finalText.charAt(i);
            MsdfGlyph glyph = this.glyphs.get(_char);
            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault(_char, 0.0f) * size * (1f + thickness);
            }
            width += glyph.getWidth(size) * (1f + thickness);
            prevChar = _char;
        }
        return width;
    }

    public static FontBuilder builder() {
        return new FontBuilder();
    }
}
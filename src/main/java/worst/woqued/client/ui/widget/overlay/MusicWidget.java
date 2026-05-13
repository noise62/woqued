package worst.woqued.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.fonts.Font;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.ui.widget.Widget;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;

public class MusicWidget extends Widget {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MediaInfo mediaInfo = new MediaInfo("No Media", "Artist", new byte[0], 0, 0, false);
    private final Identifier artwork = Identifier.of("woqued", "textures/music_artwork.png");
    private final long[] lastMedia = {0};
    public IMediaSession session;
    private boolean artworkRegistered = false;

    private boolean exitValue = false;

    public MusicWidget() {
        super(10f, 250f);
        setDraggableSize(90, 130);
    }

    @Override
    public String getName() {
        return "Music Info";
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        render(event.matrixStack());
    }

    @Override
    public void render(MatrixStack matrixStack) {
        net.minecraft.client.gui.DrawContext context = new net.minecraft.client.gui.DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
        if (mc.player == null) return;

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        boolean inChat = mc.currentScreen != null && mc.currentScreen.getClass().getSimpleName().contains("Chat");

        boolean hasActiveMedia = System.currentTimeMillis() - lastMedia[0] < 5000;
        boolean isPlaying = false;
        try {
            if (mediaInfo != null && mediaInfo.getPlaying()) {
                isPlaying = true;
            }
        } catch (Exception ignored) {}

        boolean shouldShow = hasActiveMedia || inChat || isPlaying;

        if (shouldShow) {
            exitValue = true;
        } else {
            exitValue = false;
        }

        if (inChat) exitValue = true;

        if (!inChat && !exitValue && !hasActiveMedia) return;

        float animValue = exitValue ? 1f : 0f;

        float totalWidth = 90f;
        float totalHeight = 130f;
        float padding = scaled(3f);
        float borderRadius = scaled(4f);

        matrixStack.push();
        float scaleX = x + totalWidth / 2;
        float scaleY = y + totalHeight / 2;
        matrixStack.translate(scaleX, scaleY, 0);
        matrixStack.scale(animValue, animValue, 1);
        matrixStack.translate(-scaleX, -scaleY, 0);

        Color bgColor = UIColors.widgetBlur();
        RenderUtil.RECT.draw(matrixStack, x, y, totalWidth, totalHeight, borderRadius, bgColor);

        float coverSize = totalWidth - padding * 2;
        float coverX = x + padding;
        float coverY = y + padding;

        if (artworkRegistered && mediaInfo.getArtworkPng() != null && mediaInfo.getArtworkPng().length > 0) {
            try {
                float coverRadius = scaled(4f);

                RenderUtil.RECT.draw(matrixStack, coverX, coverY, coverSize, coverSize, coverRadius, Color.WHITE);

                context.enableScissor((int)coverX, (int)coverY, (int)(coverX + coverSize), (int)(coverY + coverSize));

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                RenderSystem.setShaderTexture(0, artwork);
                context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                    artwork,
                    (int)coverX,
                    (int)coverY,
                    0, 0,
                    (int)coverSize,
                    (int)coverSize,
                    (int)coverSize,
                    (int)coverSize);

                RenderSystem.disableBlend();
                context.disableScissor();
            } catch (Exception e) {
                artworkRegistered = false;
            }
        }

        float controlPanelHeight = scaled(20f);
        float controlPanelY = coverY + coverSize + padding;

        Color panelColor = UIColors.blur();
        RenderUtil.RECT.draw(matrixStack, x + padding, controlPanelY, totalWidth - padding * 2, controlPanelHeight, scaled(3f), panelColor);

        float buttonSize = scaled(16f);
        float buttonSpacing = scaled(8f);
        float totalButtonsWidth = buttonSize * 3 + buttonSpacing * 2;
        float buttonsStartX = x + (totalWidth - totalButtonsWidth) / 2;
        float buttonY = controlPanelY + (controlPanelHeight - buttonSize) / 2 + scaled(8f);

        Font iconFont = Fonts.ICONS;

        float prevX = buttonsStartX;
        String prevIcon = "E";
        Color primaryColor = UIColors.primary();
        iconFont.drawText(matrixStack, prevIcon, prevX + (buttonSize - iconFont.getWidth(prevIcon, buttonSize)) / 2, buttonY + (buttonSize - iconFont.getHeight(buttonSize)) / 2, buttonSize, primaryColor);

        float playX = prevX + buttonSize + buttonSpacing;
        String playPauseIcon = "D";
        try {
            if (mediaInfo != null && mediaInfo.getPlaying()) {
                playPauseIcon = "C";
            }
        } catch (Exception ignored) {}
        iconFont.drawText(matrixStack, playPauseIcon, playX + (buttonSize - iconFont.getWidth(playPauseIcon, buttonSize)) / 2, buttonY + (buttonSize - iconFont.getHeight(buttonSize)) / 2, buttonSize, primaryColor);

        float nextX = playX + buttonSize + buttonSpacing;
        String nextIcon = "B";
        iconFont.drawText(matrixStack, nextIcon, nextX + (buttonSize - iconFont.getWidth(nextIcon, buttonSize)) / 2, buttonY + (buttonSize - iconFont.getHeight(buttonSize)) / 2, buttonSize, primaryColor);

        float progressPanelHeight = scaled(10f);
        float progressPanelY = controlPanelY + controlPanelHeight + padding;

        RenderUtil.RECT.draw(matrixStack, x + padding, progressPanelY, totalWidth - padding * 2, progressPanelHeight, scaled(3f), panelColor);

        float progressHeight = scaled(3f);
        float progressPadding = scaled(6f);
        float progressY = progressPanelY + (progressPanelHeight - progressHeight) / 2;
        float progressWidth = totalWidth - padding * 2 - progressPadding * 2;
        float progressX = x + padding + progressPadding;

        float progress = 0f;
        try {
            if (mediaInfo != null && mediaInfo.getDuration() > 0) {
                progress = (float) mediaInfo.getPosition() / mediaInfo.getDuration();
            }
        } catch (Exception ignored) {}

        if (progress > 0) {
            RenderUtil.RECT.draw(matrixStack, progressX, progressY, progressWidth * Math.min(1f, progress), progressHeight, scaled(1.5f), primaryColor);
        }

        matrixStack.pop();

        getDraggable().setWidth((int) totalWidth);
        getDraggable().setHeight((int) totalHeight);
    }

    public void tick() {
        if (mc.player == null) return;

        if (mc.player.age % 5 == 0) {
            executorService.execute(() -> {
                try {
                    var sessions = MediaPlayerInfo.Instance.getMediaSessions();
                    if (sessions == null || sessions.isEmpty()) {
                        return;
                    }

                    IMediaSession currentSession = sessions.stream()
                            .filter(s -> s != null && s.getMedia() != null)
                            .max(Comparator.comparing(s -> {
                                try {
                                    return s.getMedia().getPlaying();
                                } catch (Exception e) {
                                    return false;
                                }
                            }))
                            .orElse(null);

                    if (currentSession != null) {
                        try {
                            MediaInfo info = currentSession.getMedia();
                            if (info == null) {
                                return;
                            }

                            if (!info.getTitle().isEmpty() || !info.getArtist().isEmpty() || info.getPlaying()) {
                                boolean needsUpdate = mediaInfo.getTitle().equals("No Media") ||
                                    !Arrays.equals(mediaInfo.getArtworkPng(), info.getArtworkPng());

                                if (needsUpdate && info.getArtworkPng() != null && info.getArtworkPng().length > 0) {
                                    try {
                                        mc.execute(() -> {
                                            try {
                                                NativeImageBackedTexture texture = new NativeImageBackedTexture(
                                                    NativeImage.read(new java.io.ByteArrayInputStream(info.getArtworkPng()))
                                                );
                                                mc.getTextureManager().registerTexture(artwork, texture);
                                                artworkRegistered = true;
                                            } catch (Exception e) {
                                                artworkRegistered = false;
                                            }
                                        });
                                    } catch (Exception e) {
                                        artworkRegistered = false;
                                    }
                                }

                                mediaInfo = info;
                                session = currentSession;
                                lastMedia[0] = System.currentTimeMillis();
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });
        }
    }
}
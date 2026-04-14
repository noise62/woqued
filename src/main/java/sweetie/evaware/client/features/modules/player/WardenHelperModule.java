package sweetie.evaware.client.features.modules.player;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.GameLoopEvent;
import sweetie.evaware.api.event.events.client.KeyEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BindSetting;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.utils.math.ProjectionUtil;
import sweetie.evaware.api.utils.render.fonts.Font;
import sweetie.evaware.api.utils.render.fonts.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "Warden Helper", category = Category.PLAYER)
public class WardenHelperModule extends Module {
    @Getter private static final WardenHelperModule instance = new WardenHelperModule();

    private final BooleanSetting showESP = new BooleanSetting("ESP").value(true);
    private final BooleanSetting showLines = new BooleanSetting("Линии до сундуков").value(true);
    private final BooleanSetting showDistance = new BooleanSetting("Дистанция").value(true);
    private final BindSetting resetBind = new BindSetting("Ресет сундуков");

    private final Map<BlockPos, ChestData> chestDataMap = new ConcurrentHashMap<>();
    
    // Кэшированные данные для рендера (обновляются раз в SCAN_INTERVAL_MS)
    private final List<ChestRenderData> cachedChestsToRender = new ArrayList<>();
    private long lastScanTime = 0;
    private static final long SCAN_INTERVAL_MS = 500; // Сканируем раз в 0.5 секунды

    private static final double WARDEN_X = 2000.0;
    private static final double WARDEN_Z = 2000.0;
    private static final double ZONE_RADIUS = 250.0;

    private static final Color COLOR_INACTIVE = new Color(180, 180, 180, 200);
    private static final Color COLOR_DEFAULT = new Color(255, 60, 60, 200);
    private static final Color COLOR_WARN = new Color(255, 220, 50, 200);
    private static final Color COLOR_READY = new Color(60, 255, 60, 200);
    private static final Color LINE_COLOR = new Color(255, 255, 255, 150);

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(с|s|сек|sec)");

    public WardenHelperModule() {
        addSettings(showESP, showLines, showDistance, resetBind);
    }

    @Override
    public void onEnable() {
        clearAll();
    }

    @Override
    public void onDisable() {
        clearAll();
    }

    @Override
    public void onEvent() {
        EventListener gameLoopEvent = GameLoopEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || !isInWardenZone()) return;
            
            long currentTime = System.currentTimeMillis();
            boolean shouldScan = currentTime - lastScanTime >= SCAN_INTERVAL_MS;
            
            if (shouldScan) {
                scanChests();
                lastScanTime = currentTime;
            }
            updateTimersFromEntities();
        }));

        EventListener keyPressEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (resetBind.getValue() == event.key() && event.action() == 1) {
                clearAll();
            }
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null || chestDataMap.isEmpty()) return;

            // Обновляем кэш данных для рендера
            updateCachedRenderData();
            
            if (cachedChestsToRender.isEmpty()) return;

            MatrixStack matrices = event.matrixStack();
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d cam = camera.getPos();
            Vec3d eyePos = mc.player.getEyePos();

            // Рендер ESP боксов
            if (showESP.getValue()) {
                try {
                    RenderSystem.enableBlend();
                    RenderSystem.disableDepthTest();
                    RenderSystem.disableCull();
                    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

                    BufferBuilder quadsBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                    BufferBuilder linesBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                    for (ChestRenderData chest : cachedChestsToRender) {
                        Box offsetBox = new Box(
                            chest.box.minX - cam.x, chest.box.minY - cam.y, chest.box.minZ - cam.z,
                            chest.box.maxX - cam.x, chest.box.maxY - cam.y, chest.box.maxZ - cam.z
                        );

                        renderFilledBox(matrices, quadsBuffer, offsetBox, withAlpha(chest.color, 50));
                        renderOutlinedBox(matrices, linesBuffer, offsetBox, withAlpha(chest.color, 200));
                        renderBoxInternalDiagonals(matrices, linesBuffer, offsetBox, withAlpha(chest.color, 120));
                    }

                    BufferRenderer.drawWithGlobalProgram(quadsBuffer.end());
                    BufferRenderer.drawWithGlobalProgram(linesBuffer.end());

                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableCull();
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableBlend();
                } catch (IllegalStateException e) {
                    // Игнорируем ошибки пустого буфера
                }
            }

            // Рендер линий от глаз до сундуков
            if (showLines.getValue()) {
                try {
                    RenderSystem.enableBlend();
                    RenderSystem.disableDepthTest();
                    RenderSystem.disableCull();
                    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

                    BufferBuilder linesBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                    var matrix = matrices.peek().getPositionMatrix();
                    float r = LINE_COLOR.getRed() / 255f;
                    float g = LINE_COLOR.getGreen() / 255f;
                    float b = LINE_COLOR.getBlue() / 255f;
                    float a = LINE_COLOR.getAlpha() / 255f;

                    for (ChestRenderData chest : cachedChestsToRender) {
                        Vec3d chestCenter = chest.box.getCenter();
                        
                        // Линия от глаз до центра сундука
                        linesBuffer.vertex(matrix, (float)(eyePos.x - cam.x), (float)(eyePos.y - cam.y), (float)(eyePos.z - cam.z)).color(r, g, b, a);
                        linesBuffer.vertex(matrix, (float)(chestCenter.x - cam.x), (float)(chestCenter.y - cam.y), (float)(chestCenter.z - cam.z)).color(r, g, b, a);
                    }

                    BufferRenderer.drawWithGlobalProgram(linesBuffer.end());

                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableCull();
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableBlend();
                } catch (IllegalStateException e) {
                    // Игнорируем ошибки пустого буфера
                }
            }

            // Рендер дистанции над сундуками
            if (showDistance.getValue()) {
                TextRenderer textRenderer = mc.textRenderer;
                VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
                
                for (ChestRenderData chest : cachedChestsToRender) {
                    Vec3d chestCenter = chest.box.getCenter();
                    double distance = mc.player.getEyePos().distanceTo(chestCenter);
                    
                    // Получаем 2D координаты из 3D позиции
                    Vec3d textPos = new Vec3d(chestCenter.x, chestCenter.y + 1.2, chestCenter.z);
                    Vec3d projected = textPos.subtract(cam);
                    
                    // Проверяем, что сундук перед камерой
                    if (projected.z > 0) {
                        // Преобразуем в экранные координаты
                        double screenX = (projected.x / projected.z) * (mc.getWindow().getFramebufferWidth() / (2.0 * Math.tan(Math.toRadians(mc.gameRenderer.getFov(mc.gameRenderer.getCamera(), 1.0f, false))))) + mc.getWindow().getFramebufferWidth() / 2.0;
                        double screenY = mc.getWindow().getFramebufferHeight() / 2.0 - (projected.y / projected.z) * (mc.getWindow().getFramebufferHeight() / (2.0 * Math.tan(Math.toRadians(mc.gameRenderer.getFov(mc.gameRenderer.getCamera(), 1.0f, false)))));
                        
                        // Корректируем под обычный размер окна
                        screenX = screenX * mc.getWindow().getWidth() / mc.getWindow().getFramebufferWidth();
                        screenY = screenY * mc.getWindow().getHeight() / mc.getWindow().getFramebufferHeight();
                        
                        // Проверяем, что текст на экране
                        if (screenX > -100 && screenX < mc.getWindow().getWidth() + 100 && screenY > -100 && screenY < mc.getWindow().getHeight() + 100) {
                            String distanceText = String.format("%.1fm", distance);
                            int textWidth = textRenderer.getWidth(distanceText);
                            
                            matrices.push();
                            Entry matrixEntry = matrices.peek();
                            // Рендерим текст
                            textRenderer.draw(distanceText, 
                                (float)(screenX - textWidth / 2.0), 
                                (float)(screenY - textRenderer.fontHeight), 
                                0xFFFFFF, 
                                false, 
                                matrixEntry.getPositionMatrix(), 
                                vertexConsumers, 
                                TextRenderer.TextLayerType.NORMAL, 
                                0, 
                                15728880);
                            vertexConsumers.draw();
                            matrices.pop();
                        }
                    }
                }
            }
        }));

        addEvents(gameLoopEvent, keyPressEvent, renderEvent);
    }

    private void clearAll() {
        chestDataMap.clear();
        cachedChestsToRender.clear();
        lastScanTime = 0;
    }

    private void scanChests() {
        int renderDistance = 64;
        BlockPos playerPos = mc.player.getBlockPos();
        Set<BlockPos> currentChests = new HashSet<>();

        for (int x = playerPos.getX() - renderDistance; x <= playerPos.getX() + renderDistance; x++) {
            for (int y = playerPos.getY() - 10; y <= playerPos.getY() + 10; y++) {
                for (int z = playerPos.getZ() - renderDistance; z <= playerPos.getZ() + renderDistance; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = mc.world.getBlockEntity(pos);

                    if (isWardenChest(blockEntity)) {
                        currentChests.add(pos);
                        chestDataMap.computeIfAbsent(pos, ChestData::new);
                    }
                }
            }
        }

        // Удаляем сундуки, которых больше нет
        chestDataMap.keySet().removeIf(pos -> !currentChests.contains(pos));
        
        // Обновляем кэш для рендера
        updateCachedRenderData();
    }

    private void updateCachedRenderData() {
        cachedChestsToRender.clear();
        
        for (Map.Entry<BlockPos, ChestData> entry : chestDataMap.entrySet()) {
            BlockPos pos = entry.getKey();
            ChestData data = entry.getValue();
            if (data == null) continue;

            Color color = getESPColor(data);
            Box box = new Box(pos);
            cachedChestsToRender.add(new ChestRenderData(color, box));
        }
    }

    private boolean isWardenChest(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof TrappedChestBlockEntity;
    }

    private boolean isInWardenZone() {
        return Math.abs(mc.player.getX() - WARDEN_X) <= ZONE_RADIUS
                && Math.abs(mc.player.getZ() - WARDEN_Z) <= ZONE_RADIUS;
    }

    private void updateTimersFromEntities() {
        for (Map.Entry<BlockPos, ChestData> entry : chestDataMap.entrySet()) {
            BlockPos pos = entry.getKey();
            ChestData data = entry.getValue();
            boolean found = false;

            Box searchBox = new Box(pos).expand(1.0, 3.0, 1.0);
            for (ArmorStandEntity stand : mc.world.getEntitiesByClass(
                    ArmorStandEntity.class, searchBox, ArmorStandEntity::hasCustomName)) {

                String name = stand.getCustomName().getString()
                        .replaceAll("§[0-9a-fk-or]", "");
                long seconds = parseTimeToSeconds(name);
                if (seconds >= 0) {
                    data.setTimer(seconds);
                    found = true;
                    break;
                }
            }

            if (!found) data.hasTimer = false;
        }
    }

    private long parseTimeToSeconds(String str) {
        Matcher m1 = TIME_PATTERN.matcher(str);
        if (m1.find()) {
            if (m1.group(3) == null)
                return (long) Integer.parseInt(m1.group(1)) * 60 + Integer.parseInt(m1.group(2));
            return (long) Integer.parseInt(m1.group(1)) * 3600
                    + (long) Integer.parseInt(m1.group(2)) * 60
                    + Integer.parseInt(m1.group(3));
        }
        Matcher m2 = SECONDS_PATTERN.matcher(str);
        if (m2.find()) return Long.parseLong(m2.group(1));

        String digits = str.replaceAll("[^0-9]", "").trim();
        if (digits.isEmpty()) return -1L;
        long val = Long.parseLong(digits);
        return (val > 0 && val < 36000) ? val : -1L;
    }

    private Color getESPColor(ChestData data) {
        if (!data.hasTimer) return COLOR_INACTIVE;
        float t = data.getTimeLeft();
        if (t <= 0.0f) {
            float p = (float) ((Math.sin(System.currentTimeMillis() / 200.0) * 0.3) + 0.7);
            return new Color(0, (int)(255 * p), (int)(128 * p), 220);
        }
        if (t > 120.0f) return COLOR_DEFAULT;
        if (t > 20.0f) return lerpColor(COLOR_DEFAULT, COLOR_WARN, 1.0f - ((t - 20.0f) / 100.0f));
        return lerpColor(COLOR_WARN, COLOR_READY, 1.0f - (t / 20.0f));
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private void renderFilledBox(MatrixStack matrices, BufferBuilder buffer, Box box, Color color) {
        var matrix = matrices.peek().getPositionMatrix();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        // Bottom face
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        // Top face
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        // North face
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        // South face
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

        // East face
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        // West face
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
    }

    private void renderOutlinedBox(MatrixStack matrices, BufferBuilder buffer, Box box, Color color) {
        var matrix = matrices.peek().getPositionMatrix();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        // Bottom edges
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        // Top edges
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        // Vertical edges
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
    }

    private void renderBoxInternalDiagonals(MatrixStack matrices, BufferBuilder buffer, Box box, Color color) {
        var matrix = matrices.peek().getPositionMatrix();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
    }

    private static class ChestData {
        boolean hasTimer = false;
        private long timerEndMs = 0L;

        ChestData(BlockPos pos) {}

        void setTimer(long secondsLeft) {
            this.timerEndMs = System.currentTimeMillis() + secondsLeft * 1000L;
            this.hasTimer = true;
        }

        float getTimeLeft() {
            return (timerEndMs - System.currentTimeMillis()) / 1000.0f;
        }
    }

    private static class ChestRenderData {
        final Color color;
        final Box box;

        ChestRenderData(Color color, Box box) {
            this.color = color;
            this.box = box;
        }
    }
}

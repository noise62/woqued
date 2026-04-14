package sweetie.evaware.client.features.modules.player;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.GameLoopEvent;
import sweetie.evaware.api.event.events.client.KeyEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BindSetting;
import sweetie.evaware.api.module.setting.BooleanSetting;

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

    private final BooleanSetting autoGPS = new BooleanSetting("Авто GPS").value(true);
    private final BooleanSetting notifications = new BooleanSetting("Уведомления").value(true);
    private final BindSetting resetBind = new BindSetting("Ресет сундуков");

    private final Map<BlockPos, ChestData> chestDataMap = new ConcurrentHashMap<>();
    private final Set<BlockPos> gpsVisited = new HashSet<>();
    private final Set<BlockPos> notified = new HashSet<>();
    private BlockPos currentGPSTarget = null;

    private static final double WARDEN_X = 2000.0;
    private static final double WARDEN_Z = 2000.0;
    private static final double ZONE_RADIUS = 250.0;

    private static final Color COLOR_INACTIVE = new Color(180, 180, 180, 120);
    private static final Color COLOR_DEFAULT = new Color(255, 60, 60, 180);
    private static final Color COLOR_WARN = new Color(255, 220, 50, 180);
    private static final Color COLOR_READY = new Color(60, 255, 60, 180);

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(с|s|сек|sec)");

    public WardenHelperModule() {
        addSettings(autoGPS, notifications, resetBind);
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
            updateTimersFromEntities();
            updateGPSAndNotifications();
        }));

        EventListener keyPressEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (resetBind.getValue() == event.key() && event.action() == 1) {
                clearAll();
            }
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            MatrixStack matrices = event.matrixStack();
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d cam = camera.getPos();

            // Render chests
            // First, collect all current chest positions
            Set<BlockPos> currentChests = new HashSet<>();
            int renderDistance = 64;
            BlockPos playerPos = mc.player.getBlockPos();

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

            // Remove chests that no longer exist
            chestDataMap.keySet().removeIf(pos -> !currentChests.contains(pos));

            // Now collect chest data for rendering
            List<ChestRenderData> chestsToRender = new ArrayList<>();

            for (BlockPos pos : currentChests) {
                ChestData data = chestDataMap.get(pos);
                if (data == null) continue;
                
                Color color = getESPColor(data);
                Box box = new Box(pos).offset(-cam.x, -cam.y, -cam.z);
                chestsToRender.add(new ChestRenderData(color, box));
            }
            
            // Only render if we found chests
            if (chestsToRender.isEmpty()) {
                return;
            }

            try {
                RenderSystem.enableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.disableCull();
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

                BufferBuilder quadsBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                BufferBuilder linesBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                for (ChestRenderData chest : chestsToRender) {
                    renderFilledBox(matrices, quadsBuffer, chest.box, withAlpha(chest.color, 50));
                    renderOutlinedBox(matrices, linesBuffer, chest.box, withAlpha(chest.color, 200));
                    renderBoxInternalDiagonals(matrices, linesBuffer, chest.box, withAlpha(chest.color, 120));
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
        }));

        addEvents(gameLoopEvent, keyPressEvent, renderEvent);
    }

    private void clearAll() {
        chestDataMap.clear();
        gpsVisited.clear();
        notified.clear();
        currentGPSTarget = null;
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

    private void updateGPSAndNotifications() {
        if (currentGPSTarget != null
                && mc.player.squaredDistanceTo(Vec3d.ofCenter(currentGPSTarget)) <= 100.0) {
            mc.player.networkHandler.sendChatCommand("gps off");
            currentGPSTarget = null;
        }

        for (Map.Entry<BlockPos, ChestData> entry : chestDataMap.entrySet()) {
            BlockPos pos = entry.getKey();
            ChestData data = entry.getValue();
            if (!data.hasTimer) continue;

            float t = data.getTimeLeft();

            if (t <= 0.0f) {
                if (autoGPS.getValue() && !gpsVisited.contains(pos))
                    sendGPS(pos);
                if (notifications.getValue() && !notified.contains(pos)) {
                    notified.add(pos);
                }
            } else if (t <= 20.0f) {
                if (autoGPS.getValue() && !gpsVisited.contains(pos))
                    sendGPS(pos);
                if (notifications.getValue() && !notified.contains(pos)) {
                    notified.add(pos);
                }
            }
        }
    }

    private void sendGPS(BlockPos pos) {
        mc.player.networkHandler.sendChatCommand("gps set " + pos.getX() + " " + pos.getZ());
        gpsVisited.add(pos);
        currentGPSTarget = pos;
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

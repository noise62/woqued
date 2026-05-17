package worst.woqued.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.animation.AnimationUtil;
import worst.woqued.api.utils.animation.Easing;
import worst.woqued.api.utils.color.ColorUtil;
import worst.woqued.api.utils.color.UIColors;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleRegister(name = "Line Glyphs", category = Category.RENDER)
public class LineGlyphsModule extends Module {
    @Getter private static final LineGlyphsModule instance = new LineGlyphsModule();

    private final SliderSetting count    = new SliderSetting("Count").value(70f).range(10f, 200f).step(5f);
    private final BooleanSetting slow    = new BooleanSetting("Slow Speed").value(false);
    private final ModeSetting colorMode  = new ModeSetting("Color").value("Theme").values("Theme", "Rainbow", "Single");

    private final List<Glyph> glyphs = new ArrayList<>();
    private final Random rand = new Random(93882L);

    public LineGlyphsModule() {
        addSettings(count, slow, colorMode);
    }

    @Override
    public void onEnable() {
        glyphs.clear();
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(e -> {
            if (mc.player == null) return;
            glyphs.removeIf(g -> g.isDead());
            int cap = count.getValue().intValue();
            int attempts = 8;
            while (attempts-- > 0 && glyphs.size() < cap) {
                glyphs.add(new Glyph(spawnPos(), rand.nextInt(7, 13)));
            }
            glyphs.forEach(Glyph::tick);
        }));

        EventListener render = Render3DEvent.getInstance().subscribe(new Listener<>(e -> {
            if (mc.player == null) return;
            drawAll(e.matrixStack(), e.partialTicks());
        }));

        addEvents(tick, render);
    }


    private void drawAll(MatrixStack ms, float pt) {
        if (glyphs.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        int idx = 0;
        for (Glyph g : glyphs) {
            List<Vec3d> pts = g.getPoints(pt);
            if (pts.size() < 2) { idx++; continue; }

            float lineW = calcLineWidth(g, cam);
            RenderSystem.lineWidth(lineW);

            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            Matrix4f mat = ms.peek().getPositionMatrix();
            int ci = idx;
            int step = 0;
            for (Vec3d p : pts) {
                float apc = g.alpha() * (0.25f + (float) step / pts.size() / 1.75f);
                Color c = stateColor(ci, apc);
                buf.vertex(mat,
                                (float)(p.x - cam.x),
                                (float)(p.y - cam.y),
                                (float)(p.z - cam.z))
                        .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
                ci += 180;
                step++;
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());

            RenderSystem.lineWidth(lineW * 3f);
            BufferBuilder dots = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            ci = idx;
            step = 0;
            for (Vec3d p : pts) {
                float apc = g.alpha() * (0.25f + (float) step / pts.size() / 1.75f);
                Color c = stateColor(ci, apc);
                dots.vertex(mat,
                                (float)(p.x - cam.x),
                                (float)(p.y - cam.y),
                                (float)(p.z - cam.z))
                        .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
                ci += 180;
                step++;
            }
            BufferRenderer.drawWithGlobalProgram(dots.end());

            idx++;
        }

        RenderSystem.lineWidth(1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private Color stateColor(int index, float alphaPC) {
        Color base = switch (colorMode.getValue()) {
            case "Rainbow" -> {
                float hue = ((System.currentTimeMillis() / 20f + index * 2f) % 360f) / 360f;
                yield Color.getHSBColor(hue, 1f, 1f);
            }
            case "Single"  -> UIColors.primary();
            default        -> UIColors.gradient(index); // Theme
        };
        int a = Math.max(0, Math.min(255, (int)(alphaPC * 255f)));
        return ColorUtil.setAlpha(base, a);
    }

    private float calcLineWidth(Glyph g, Vec3d cam) {
        if (g.nodes.isEmpty()) return 1f;
        int[] n0 = g.nodes.get(0);
        Vec3d first = new Vec3d(n0[0], n0[1], n0[2]);
        double dst = cam.distanceTo(first);
        return 1e-4f + 3f * (float) Math.max(0, Math.min(1, 1.0 - dst / 20.0));
    }


    private int[] spawnPosInts() {
        double fov = mc.options.getFov().getValue();
        double yaw = Math.toRadians(rand.nextInt(
                (int)(mc.player.getYaw() - fov * 0.75),
                (int)(mc.player.getYaw() + fov * 0.75)));
        double dst = rand.nextInt(6, 24);
        int dx = (int)(-(Math.sin(yaw) * dst));
        int dy = rand.nextInt(0, 12);
        int dz = (int)(Math.cos(yaw) * dst);
        Vec3d eye = mc.player.getEyePos();
        return new int[]{(int)eye.x + dx, (int)eye.y + dy, (int)eye.z + dz};
    }

    private int[] spawnPos() { return spawnPosInts(); }


    private int[] randXY() {
        return new int[]{rand.nextInt(0, 4) * 90, rand.nextInt(-1, 2) * 90};
    }

    private int[] nextDir(int[] prev) {
        int a = prev[0], b = prev[1];
        int nb = b;
        for (int i = 150; i > 0 && Math.abs(nb - b) != 90; i--) nb = rand.nextInt(-2, 2) * 90;
        int na = a;
        for (int i = 5; i > 0 && Math.abs(na - a) != 90; i--) na = rand.nextInt(0, 4) * 90;
        return new int[]{na, nb};
    }

    private int[] step(int[] pos, int[] dir, int r) {
        double yaw   = Math.toRadians(dir[0]);
        double pitch = Math.toRadians(dir[1]);
        double r1 = r;
        int ry = (int)(Math.sin(pitch) * r1);
        if (pitch != 0) r1 = 0;
        int rx = (int)(-(Math.sin(yaw) * r1));
        int rz = (int)(Math.cos(yaw) * r1);
        return new int[]{pos[0] + rx, pos[1] + ry, pos[2] + rz};
    }


    private class Glyph {
        final List<int[]> nodes = new ArrayList<>();
        private int[] dir;
        private int stepsLeft;
        private int ticksLeft;
        private int lastSet;
        private boolean dying = false;
        private final AnimationUtil anim = new AnimationUtil();

        Glyph(int[] spawn, int steps) {
            nodes.add(spawn);
            dir = randXY();
            stepsLeft = steps;
            anim.setValue(0.0);
            anim.run(1.0, 600, Easing.SINE_OUT);
        }

        void tick() {
            anim.update();
            if (stepsLeft == 0) {
                if (!dying) {
                    dying = true;
                    anim.run(0.0, 400, Easing.SINE_IN);
                }
                return;
            }
            if (ticksLeft > 0) {
                ticksLeft -= slow.getValue() ? 1 : 2;
                if (ticksLeft < 0) ticksLeft = 0;
                return;
            }
            dir = nextDir(dir);
            lastSet = ticksLeft = rand.nextInt(0, 3);
            nodes.add(step(nodes.get(nodes.size() - 1), dir, Math.max(1, ticksLeft)));
            stepsLeft--;
        }

        float alpha() { return (float) anim.getValue(); }

        boolean isDead() {
            return stepsLeft == 0 && anim.getValue() < 0.01 && anim.isFinished();
        }

        List<Vec3d> getPoints(float pt) {
            List<Vec3d> out = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i++) {
                int[] n = nodes.get(i);
                double x = n[0], y = n[1], z = n[2];
                // smooth last segment
                if (i == nodes.size() - 1 && nodes.size() >= 2) {
                    int[] prev = nodes.get(i - 1);
                    float adv = lastSet > 0 ? Math.max(0, Math.min(1, 1f - (float) ticksLeft / lastSet)) : 1f;
                    x = lerp(prev[0], x, adv);
                    y = lerp(prev[1], y, adv);
                    z = lerp(prev[2], z, adv);
                }
                out.add(new Vec3d(x, y, z));
            }
            return out;
        }

        private double lerp(double a, double b, float t) { return a + (b - a) * t; }
    }
}
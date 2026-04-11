package sweetie.evaware.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.*;
import sweetie.evaware.api.system.configs.FriendManager;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.combat.TargetManager;
import sweetie.evaware.api.utils.math.MathUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

@ModuleRegister(name = "Pointers", category = Category.RENDER)
public class PointersModule extends Module {
    @Getter private static final PointersModule instance = new PointersModule();

    protected final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Animals").value(false),
            new BooleanSetting("Mobs").value(false),
            new BooleanSetting("Items").value(false)
    );

    private final ModeSetting playerModeC = new ModeSetting("Players mode").value("Client").values("Client", "Custom").setVisible(() -> targets.isEnabled("Players"));
    private final ModeSetting animalsModeC = new ModeSetting("Animals mode").value("Client").values("Client", "Custom").setVisible(() -> targets.isEnabled("Animals"));
    private final ModeSetting mobModeC = new ModeSetting("Mobs mode").value("Client").values("Client", "Custom").setVisible(() -> targets.isEnabled("Mobs"));
    private final ModeSetting friendModeC = new ModeSetting("Friends mode").value("Client").values("Client", "Custom").setVisible(() -> targets.isEnabled("Players"));
    private final ModeSetting itemModeC = new ModeSetting("Items mode").value("Client").values("Client", "Custom").setVisible(() -> targets.isEnabled("Items"));

    private final ColorSetting playerColor = new ColorSetting("Player color").value(new Color(-1)).setVisible(() -> playerModeC.is("Custom") && targets.isEnabled("Players"));
    private final ColorSetting animalsColor = new ColorSetting("Animals color").value(new Color(-1)).setVisible(() -> animalsModeC.is("Custom") && targets.isEnabled("Animals"));
    private final ColorSetting mobColor = new ColorSetting("Mobs color").value(new Color(-1)).setVisible(() -> mobModeC.is("Custom") && targets.isEnabled("Mobs"));
    private final ColorSetting friendColor = new ColorSetting("Friends color").value(new Color(94, 255, 69)).setVisible(() -> friendModeC.is("Custom") && targets.isEnabled("Players"));
    private final ColorSetting itemColor = new ColorSetting("Items color").value(new Color(255, 72, 69)).setVisible(() -> itemModeC.is("Custom") && targets.isEnabled("Items"));

    private final SliderSetting pointerSize = new SliderSetting("Size").value(1f).range(0.5f, 2.5f).step(0.1f);
    private final SliderSetting pointerRadius = new SliderSetting("Radius").value(40f).range(20f, 100f).step(1f);

    private final ModeSetting animation = new ModeSetting("Animation").value("In").values("Out", "In", "None");
    private final SliderSetting duration = new SliderSetting("Duration").value(4f).range(1f, 20f).step(1f);

    private final TargetManager.EntityFilter entityFilter = new TargetManager.EntityFilter(targets.getList());

    private final HashSet<Entity> alive = new HashSet<>();
    private final HashMap<Entity, AnimationUtil> animations = new HashMap<>();
    private final AnimationUtil yawAnimation = new AnimationUtil();
    private final AnimationUtil radiusAnimation = new AnimationUtil();

    public PointersModule() {
        addSettings(targets,
                playerModeC, animalsModeC, mobModeC, friendModeC, itemModeC,
                playerColor, animalsColor, mobColor, friendColor, itemColor,
                pointerSize, pointerRadius, animation, duration
                );
    }

    @Override
    public void onDisable() {
        alive.clear();
        animations.clear();
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(2, event -> {
            alive.clear();

            yawAnimation.update();
            radiusAnimation.update();

            yawAnimation.run(mc.player.getYaw(), 200, Easing.EXPO_OUT);
            radiusAnimation.run(getContainerSize(), 300, Easing.EXPO_OUT);

            for (Entity entity : mc.world.getEntities()) {
                entityFilter.targetSettings = targets.getList();
                entityFilter.needFriends = true;

                if (mc.player == entity) continue;

                if ((entity instanceof ItemEntity && targets.isEnabled("Items")) ||
                        (entity instanceof LivingEntity le && entityFilter.isValid(le))) {
                    alive.add(entity);
                }
            }

            for (Entity entity : alive) {
                if (!animations.containsKey(entity)) {
                    animations.put(entity, new AnimationUtil());
                }
            }

            Iterator<Map.Entry<Entity, AnimationUtil>> iterator = animations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Entity, AnimationUtil> entry = iterator.next();
                Entity entity = entry.getKey();
                AnimationUtil anim = entry.getValue();

                boolean isAlive = alive.contains(entity);
                anim.update();
                anim.run(isAlive ? 1.0 : 0.0, duration.getValue().longValue() * 50, Easing.SINE_OUT);

                drawPointerToEntity(event, entity, anim);

                if (!isAlive && anim.getValue() <= 0.0) {
                    iterator.remove();
                }
            }
        }));

        addEvents(renderEvent);
    }

    private void drawPointerToEntity(Render2DEvent.Render2DEventData event, Entity entity, AnimationUtil spawn){
        DrawContext context = event.context();
        MatrixStack matrixStack = context.getMatrices();
        float centerX = getCenterX();
        float centerY = getCenterY();

        float animFactor = 1f;
        float spawnAnim = (float) spawn.getValue();

        if (animation.is("In")) {
            animFactor = (2f - spawnAnim);
        } else if (animation.is("Out")) {
            animFactor = 0.3f + 0.7f * spawnAnim;
        }

        if (spawnAnim <= 0.0) return;

        float animatedRadius = pointerRadius.getValue() * animFactor;
        float yaw = (float) (getEntityYaw(entity) - yawAnimation.getValue());

        matrixStack.translate(centerX, centerY, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? -yaw : yaw));
        matrixStack.translate(-centerX, -centerY, 0.0F);

        Color color = ColorUtil.setAlpha(getEntityColor(entity), (int) (spawnAnim * 255));
        drawPointer(context, centerX, (float) (centerY - animatedRadius - radiusAnimation.getValue()), pointerSize.getValue() * 20F, ColorUtil.setAlpha(color, (int) (255f * (color.getAlpha() / 255f) * spawn.getValue())), false);

        matrixStack.translate(centerX, centerY, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? yaw : -yaw));
        matrixStack.translate(-centerX, -centerY, 0.0F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public void drawPointer(DrawContext context, float x, float y, float size, Color color, boolean gps) {
        RenderSystem.setShaderTexture(0, FileUtil.getImage("pointers/" + (gps ? "arrow_gps" : "triangle")));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        RenderSystem.disableDepthTest();
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        float scaledSize = size + 8;
        buffer.vertex(matrix, x - (scaledSize / 2f), y + scaledSize, 0).texture(0f, 1f);
        buffer.vertex(matrix, x + scaledSize / 2f, y + scaledSize, 0).texture(1f, 1f);
        buffer.vertex(matrix, x + scaledSize / 2f, y, 0).texture(1f, 0);
        buffer.vertex(matrix, x - (scaledSize / 2f), y, 0).texture(0, 0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float getCenterX() {
        return mc.getWindow().getScaledWidth() / 2f;
    }

    private float getCenterY() {
        return mc.getWindow().getScaledHeight() / 2f;
    }

    private float getContainerSize() {
        if (mc.currentScreen instanceof HandledScreen<?> containerScreen) {
            return Math.max(containerScreen.width, containerScreen.height) * 0.05f + (pointerRadius.getMax() - pointerRadius.getValue());
        }

        return 0f;
    }

    private float getEntityYaw(Entity entity) {
        if (mc.player == null) return 0;
        double xA = (MathUtil.interpolate(mc.player.prevX, mc.player.getPos().x));
        double zA = (MathUtil.interpolate(mc.player.prevZ, mc.player.getPos().z));
        double x = MathUtil.interpolate(entity.prevX, entity.getPos().x) - xA;
        double z = MathUtil.interpolate(entity.prevZ, entity.getPos().z) - zA;
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }

    private Color getEntityColor(Entity entity) {
        int seed = entity.getId() * 13;
        Color gradient = UIColors.gradient(seed);
        return switch (entity) {
            case ItemEntity itemEntity -> itemModeC.is("Custom") ? itemColor.getValue() : gradient;
            case PlayerEntity player -> FriendManager.getInstance().contains(player.getName().getString()) ? friendModeC.is("Custom") ? friendColor.getValue() : gradient : playerModeC.is("Custom") ? playerColor.getValue() : gradient;
            case AnimalEntity animalEntity -> animalsModeC.is("Custom") ? animalsColor.getValue() : gradient;
            case MobEntity mobEntity -> mobModeC.is("Custom") ? mobColor.getValue() : gradient;
            default -> new Color(-1);
        };
    }
}

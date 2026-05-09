package worst.woqued.client.features.modules.render.nametags;

import worst.woqued.api.utils.render.display.BoxRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.event.events.render.Render2DEvent;
import worst.woqued.api.event.events.render.Render3DEvent;
import worst.woqued.api.system.configs.FriendManager;
import worst.woqued.api.system.interfaces.QuickImports;
import worst.woqued.api.utils.color.UIColors;
import worst.woqued.api.utils.math.MathUtil;
import worst.woqued.api.utils.math.ProjectionUtil;
import worst.woqued.api.utils.render.RenderUtil;
import worst.woqued.api.utils.render.display.BlurRectRender;
import worst.woqued.api.utils.render.fonts.Font;
import worst.woqued.api.utils.render.fonts.Fonts;
import worst.woqued.client.features.modules.render.InterfaceModule;

import java.awt.*;

public class NameTagsRender implements QuickImports {
    private final NameTagsModule module;
    private final NameTagsItems nameTagsItems;
    private final NameTagsPotions nameTagsPotions;

    public NameTagsRender(NameTagsModule module) {
        this.module = module;
        this.nameTagsItems = new NameTagsItems(module);
        this.nameTagsPotions = new NameTagsPotions(module);
    }

    public void onRender2D(Render2DEvent.Render2DEventData event) {
        for (Entity entity1 : mc.world.getEntities()) {
            if (entity1 instanceof PlayerEntity player) {
                if (module.entityFilter.isValid(player) ||
                        player == mc.player && module.targets.isEnabled("Self") && !mc.options.getPerspective().isFirstPerson()) {
                    renderTag(player, event.context(), event.partialTicks());
                }
            }
        }
    }

public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (!module.box3d.getValue()) return;

        for (Entity entity1 : mc.world.getEntities()) {
            if (entity1 instanceof PlayerEntity player) {
                if (module.entityFilter.isValid(player) ||
                        player == mc.player && module.targets.isEnabled("Self") && !mc.options.getPerspective().isFirstPerson()) {
                    double xI = MathHelper.lerp(event.partialTicks(), entity1.prevX, entity1.getX());
                    double yI = MathHelper.lerp(event.partialTicks(), entity1.prevY, entity1.getY());
                    double zI = MathHelper.lerp(event.partialTicks(), entity1.prevZ, entity1.getZ());
                    render3DBox(player, xI, yI, zI);
                }
            }
        }
    }

    private void renderTag(Entity entity, DrawContext context, float partialTicks) {
        double xI = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
        double yI = MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
        double zI = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());
        Box box = entity.getBoundingBox().offset(xI - entity.getX(), yI - entity.getY(), zI - entity.getZ());

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (int i = 0; i < 8; i++) {
            double cornerX = (i % 2 == 0) ? box.minX : box.maxX;
            double cornerY = ((i / 2) % 2 == 0) ? box.minY : box.maxY;
            double cornerZ = ((i / 4) % 2 == 0) ? box.minZ : box.maxZ;

            Vector2f projected = ProjectionUtil.project(new Vec3d(cornerX, cornerY, cornerZ));

            minX = Math.min(minX, projected.x);
            minY = Math.min(minY, projected.y);
            maxX = Math.max(maxX, projected.x);
            maxY = Math.max(maxY, projected.y);
        }

        float scale = module.scale.getValue();

        float x = (minX + maxX) / 2f;
        float y = minY - 20f * scale;

        boolean inRegion = x > 0 && x < mc.getWindow().getScaledWidth() || y > 0 && y < mc.getWindow().getScaledHeight();

        if (inRegion) {
            renderName(entity, x, y, context);

            if (!(entity instanceof PlayerEntity player)) return;

            if (module.information.isEnabled("Items")) {
                nameTagsItems.renderItems(player, x, y, context);
            }

            if (module.information.isEnabled("Potions")) {
                nameTagsPotions.renderPotions(player, maxX + 2f * scale, minY, context);
            }

            if (module.options.isEnabled("Special items")) {
                nameTagsItems.renderSpecialItems(player, x, maxY - 2f * scale, context);
            }
        }
    }

    private void renderName(Entity entity, float x, float y, DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();
        Font font = Fonts.SF_MEDIUM;

        String name = entity.getName().getString();
        Text prefix = null;

        if (entity instanceof PlayerEntity player) {
            prefix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getPrefix() : null;
        }

        float scale = module.scale.getValue();
        float size = 8f * scale;
        float gap = 2f * scale;
        float nameWidth = font.getWidth(name, size);
        float prefixWidth = font.getWidth(prefix, size);

        boolean hasPrefix = prefixWidth > 0.5 && prefix != null;

        float spaceWidth = !hasPrefix ? 0 : font.getWidth(" ", size);
        float textWidth = prefixWidth + spaceWidth + nameWidth;

        x -= textWidth / 2f + gap;

        Color color = !FriendManager.getInstance().contains(entity.getName().getString()) ? module.color.getValue() : module.friendColor.getValue();

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, textWidth + gap * 2f, size + gap * 2f, scale, color, 1f - module.glassy.getValue());

        float textX = x + gap;
        float textY = y + gap;

        if (hasPrefix) {
            font.drawText(matrixStack, prefix, textX, textY, size);
            textX += prefixWidth + spaceWidth;
        }

        font.drawText(matrixStack, name, textX, textY, size, module.textColor.getValue());
    }

private void render3DBox(Entity entity, double x, double y, double z) {
        float width = entity.getWidth();
        float height = entity.getHeight();

        float posX = (float) x;
        float posY = (float) y + 1f;
        float posZ = (float) z;

        Color themeColor = UIColors.primary();
        float alpha = module.boxAlpha.getValue();

        Color fillColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int) (alpha * 255));

        RenderUtil.BOX.drawBox(posX, posY, posZ, posX + width, posY + height, posZ + width, 3.0f, fillColor, BoxRender.Render.STRIPED, width / 5.5f);
    }
}
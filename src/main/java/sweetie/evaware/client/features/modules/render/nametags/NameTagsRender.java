package sweetie.evaware.client.features.modules.render.nametags;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.system.configs.FriendManager;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.ProjectionUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.display.BlurRectRender;
import sweetie.evaware.api.utils.render.fonts.Font;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.features.modules.render.InterfaceModule;

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

    public void onRender(Render2DEvent.Render2DEventData event) {
        for (Entity entity1 : mc.world.getEntities()) {
            if (entity1 instanceof LivingEntity entity) {
                if (module.entityFilter.isValid(entity) ||
                        entity == mc.player && module.targets.isEnabled("Self") && !mc.options.getPerspective().isFirstPerson()) {
                    renderTag(entity, event.context());
                }
            }
        }
    }

    private void renderTag(Entity entity, DrawContext context) {
        double xI = MathUtil.interpolate(entity.prevX, entity.getX());
        double yI = MathUtil.interpolate(entity.prevY, entity.getY());
        double zI = MathUtil.interpolate(entity.prevZ, entity.getZ());

        Box box = entity.getBoundingBox();
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;

        Box box1 = new Box(xI - sizeX / 2, yI, zI - sizeZ / 2, xI + sizeX / 2, yI + sizeY, zI + sizeZ / 2);

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (int i = 0; i < 8; i++) {
            double cornerX = (i % 2 == 0) ? box1.minX : box1.maxX;
            double cornerY = ((i / 2) % 2 == 0) ? box1.minY : box1.maxY;
            double cornerZ = ((i / 4) % 2 == 0) ? box1.minZ : box1.maxZ;

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
}

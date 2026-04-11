package sweetie.evaware.client.features.modules.render.nametags;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NameTagsItems {
    private final NameTagsModule module;

    private static final Set<String> WHITELIST_ENCHANTS = new HashSet<>(Arrays.asList(
            "minecraft:protection",
            "minecraft:fire_protection",
            "minecraft:blast_protection",
            "minecraft:projectile_protection",
            "minecraft:thorns",
            "minecraft:sharpness",
            "minecraft:fire_aspect",
            "minecraft:knockback",
            "minecraft:looting",
            "minecraft:unbreaking",
            "minecraft:efficiency",
            "minecraft:power",
            "minecraft:punch",
            "minecraft:infinity",
            "minecraft:mending"
    ));

    public NameTagsItems(NameTagsModule module) {
        this.module = module;
    }

    public void renderItems(PlayerEntity entity, float x, float y, DrawContext context) {
        List<ItemStack> items = new ArrayList<>();
        MatrixStack matrixStack = context.getMatrices();

        items.add(entity.getMainHandStack().copy());
        items.add(entity.getOffHandStack().copy());

        if (!module.options.isEnabled("Only hands")) {
            items.addAll(entity.getInventory().armor);
        }

        items.removeIf(itemStack -> itemStack.getItem() == Items.AIR);

        int itemsSize = items.size();
        float scale = module.scale.getValue();
        float gap = 2f * scale;
        float itemSize = 14f * scale;
        float itemSpacing = itemSize + gap * 2f;
        float totalWidth = itemsSize * itemSize + (itemsSize - 1) * gap;
        float startX = x - totalWidth / 2f;
        y -= itemSize + gap * 2f;

        for (int i = 0; i < itemsSize; i++) {
            matrixStack.push();
            matrixStack.translate(startX + i * itemSpacing, y, 0);
            matrixStack.scale(scale, scale, 1);

            DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            context.drawItem(items.get(i), 0, 0);
            context.drawStackOverlay(QuickImports.mc.textRenderer, items.get(i), 0, 0);

            if (module.options.isEnabled("Enchants")) {
                renderEnchantments(items.get(i), matrixStack, -gap, -itemSize / 2f);
            }

            matrixStack.pop();
        }
    }

    public void renderSpecialItems(PlayerEntity player, float x, float y, DrawContext context) {
        List<ItemStack> specialItems = new ArrayList<>();
        MatrixStack matrixStack = context.getMatrices();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            String itemName = stack.getName().getString();

            boolean isTalik = (
                        stack.getItem() == Items.TOTEM_OF_UNDYING ||
                        stack.getItem() == Items.PLAYER_HEAD ||
                        stack.getItem() == Items.POPPED_CHORUS_FRUIT
                    ) && (
                        itemName.contains("Сфера") ||
                        itemName.contains("Руна") ||
                        itemName.contains("Шар") ||
                        itemName.contains("Талисман")
                    );

            boolean isAngelElytra = itemName.contains("Крылья ангела") && stack.getItem() == Items.ELYTRA;
            boolean isKrush = itemName.contains("Круш");

            if (isAngelElytra || isKrush || isTalik) {
                specialItems.add(stack.copy());
            }
        }

        if (specialItems.isEmpty()) return;

        float scale = module.scale.getValue();
        float gap = 2f * scale;
        float textSize = 7f * scale;

        y += 5f * scale;

        for (int i = 0; i < specialItems.size(); i++) {
            String itemName = specialItems.get(i).getName().getString();
            float textWidth = Fonts.PS_MEDIUM.getWidth(itemName, textSize);
            float itemY = y + i * (gap * 2f + textSize);
            
            Fonts.PS_MEDIUM.drawText(matrixStack, itemName, x - textWidth / 2f, itemY + gap / 1.5f, textSize, new Color(255, 255, 255));
            y += 2f;
        }
    }

    private void renderEnchantments(ItemStack stack, MatrixStack matrices, float x, float y) {
        float offsetY = 0f;
        float fontSize = 5.5f * module.scale.getValue();

        if (stack.hasEnchantments()) {
            ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
                RegistryEntry<Enchantment> enchantment = entry.getKey();
                if (!WHITELIST_ENCHANTS.contains(enchantment.getIdAsString())) continue;

                int level = entry.getIntValue();
                int max = enchantment.value().getMaxLevel();

                String shortName = enchantment.getIdAsString();
                shortName = shortName.substring(shortName.indexOf(':') + 1, Math.min(shortName.indexOf(':') + 4, shortName.length()));
                shortName = shortName.substring(0, 1).toUpperCase() + shortName.substring(1);

                Color color = level < max + 1 ? UIColors.textColor() : UIColors.negativeColor();
                Fonts.PS_MEDIUM.drawText(matrices, shortName + " " + level, x, y + offsetY, fontSize, color);
                offsetY -= fontSize;
            }
        }
    }
}

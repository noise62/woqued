package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.other.SlownessManager;

@ModuleRegister(name = "Auto Totem", category = Category.COMBAT)
public class AutoTotemModule extends Module {
    @Getter private static final AutoTotemModule instance = new AutoTotemModule();

    private final SliderSetting health = new SliderSetting("Health").value(5f).range(0f, 20f).step(0.5f);
    private final MultiBooleanSetting options = new MultiBooleanSetting("Options").value(
            new BooleanSetting("Swap back").value(true),
            new BooleanSetting("No ball switch").value(false),
            new BooleanSetting("Save enchanted").value(false)
    );

    private final MultiBooleanSetting checks = new MultiBooleanSetting("Checks").value(
            new BooleanSetting("Absorption").value(true),
            new BooleanSetting("Crystals").value(true),
            new BooleanSetting("Falling").value(false),
            new BooleanSetting("Elytra").value(false)
    );
    private final SliderSetting healthWithElytra = new SliderSetting("Health with elytra").value(10f).range(0f, 20f).step(0.5f).setVisible(() -> checks.isEnabled("Elytra"));

    private final TimerUtil timerUtil = new TimerUtil();
    private int oldItem = -1;
    private boolean totemIsUsed = false;
    private int nonEnchantedTotems = 0;
    private boolean isTotemPlaced = false;

    public AutoTotemModule() {
        addSettings(health, options, checks, healthWithElytra);
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (SlownessManager.isEnabled()) return;

            updateTotemCount();
            handleTotemSwapping();
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!SlownessManager.isEnabled()) return;

            updateTotemCount();
            handleTotemSwapping();
        }));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            handleTotemUsePacket(event);
        }));

        addEvents(updateEvent, tickEvent, packetEvent);
    }

    private void updateTotemCount() {
        nonEnchantedTotems = InventoryUtil.countNonEnchantedTotems();
    }

    private void handleTotemSwapping() {
        if (!timerUtil.finished(400)) return;

        if (shouldPlaceTotem()) {
            placeTotem();
            return;
        }

        if (shouldReturnItem() && !canSwap()) {
            returnOriginalItem();
        }
    }

    private boolean shouldPlaceTotem() {
        int slot = InventoryUtil.findBestTotemSlot(options.isEnabled("Save enchanted"));
        return canSwap() && slot != -1 && !hasTotemInHand();
    }

    private boolean shouldReturnItem() {
        return oldItem != -1 && options.isEnabled("Swap back");
    }

    private void placeTotem() {
        int slot = InventoryUtil.findBestTotemSlot(options.isEnabled("Save enchanted"));
        saveCurrentItem(slot);
        swapToOffhand(slot);
        isTotemPlaced = true;
        timerUtil.reset();
    }

    private void returnOriginalItem() {
        swapToOffhand(oldItem);
        isTotemPlaced = false;
        oldItem = -1;
        timerUtil.reset();
    }

    private void saveCurrentItem(int slot) {
        if (!mc.player.getOffHandStack().isOf(Items.AIR) && oldItem == -1) {
            oldItem = slot;
        }
    }

    private void handleTotemUsePacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive()) return;

        if (event.packet() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 35 && packet.getEntity(mc.world) == mc.player) {
                totemIsUsed = true;
                isTotemPlaced = false;
            }
        }
    }

    private boolean hasTotemInHand() {
        return (mc.player.getStackInHand(Hand.MAIN_HAND).isOf(Items.TOTEM_OF_UNDYING) &&
                isNotSaveEnchanted(mc.player.getStackInHand(Hand.MAIN_HAND))) ||
                (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) &&
                        isNotSaveEnchanted(mc.player.getOffHandStack()));
    }

    private boolean isNotSaveEnchanted(ItemStack stack) {
        return !options.isEnabled("Save enchanted") || !stack.hasEnchantments() || nonEnchantedTotems <= 0;
    }

    private boolean canSwap() {
        float healthWithAbsorption = calculateEffectiveHealth();
        float finalHealth = mc.player.getInventory().getStack(38).isOf(Items.ELYTRA) && checks.isEnabled("Elytra") ? healthWithElytra.getValue() : health.getValue();

        if (isOffhandProtectedItem()) return false;
        if (isInDanger()) return true;
        return healthWithAbsorption <= finalHealth;
    }

    private float calculateEffectiveHealth() {
        float absorption = checks.isEnabled("Absorption") ? mc.player.getAbsorptionAmount() : 0f;
        return mc.player.getHealth() + absorption;
    }

    private boolean isOffhandProtectedItem() {
        if (shouldIgnoreProtection()) return false;
        return options.isEnabled("No ball switch") &&
                mc.player.getOffHandStack().isOf(Items.PLAYER_HEAD);
    }

    private boolean shouldIgnoreProtection() {
        return checks.isEnabled("Falling") && mc.player.fallDistance > 5f;
    }

    private boolean isInDanger() {
        return checkCrystals() || checkFalling();
    }

    private boolean checkCrystals() {
        if (!checks.isEnabled("Crystals")) return false;

        for (Entity entity : mc.world.getEntities()) {
            if (isDangerousEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDangerousEntity(Entity entity) {
        return (entity instanceof EndCrystalEntity || entity instanceof TntMinecartEntity) &&
                mc.player.distanceTo(entity) <= 6f;
    }

    private boolean checkFalling() {
        if (!checks.isEnabled("Falling")) return false;
        if (mc.player.isTouchingWater()) return false;
        if (mc.player.isGliding()) return false;
        return mc.player.fallDistance > 10f;
    }

    private void swapToOffhand(int slot) {
        if (SlownessManager.isEnabled()) {
            SlownessManager.applySlowness(10, () -> InventoryUtil.swapToOffhand(slot));
        } else InventoryUtil.swapToOffhand(slot);
    }

    private void resetState() {
        oldItem = -1;
        totemIsUsed = false;
        isTotemPlaced = false;
    }
}

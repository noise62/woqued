package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.*;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.other.SlownessManager;
import sweetie.evaware.api.utils.rotation.manager.*;
import sweetie.evaware.api.utils.task.TaskPriority;

import java.util.*;
import java.util.stream.StreamSupport;

@ModuleRegister(name = "Auto Buff", category = Category.COMBAT)
public class AutoBuffModule extends Module {
    @Getter private static final AutoBuffModule instance = new AutoBuffModule();

    private final MultiBooleanSetting potions = new MultiBooleanSetting("Potions").value(
            new BooleanSetting("Strength").value(true),
            new BooleanSetting("Speed").value(true),
            new BooleanSetting("Fire resistance").value(false)
    );
    private final ModeSetting mode = new ModeSetting("Mode").value("Legit").values("Legit", "Packet");
    private final SliderSetting ticks = new SliderSetting("Tick after spawn").value(10f).range(0f,200f).step(5f);
    private final BooleanSetting autoDisable = new BooleanSetting("Auto disable").value(false);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only on ground").value(true);

    private final TimerUtil throwTimer = new TimerUtil(), rotationTimer = new TimerUtil();
    private final List<String> toApply = new ArrayList<>(), toReapply = new ArrayList<>();
    private float originalPitch;
    private int originalSlot, potionInvSlot = -1, tempHotbarSlot = -1;
    private boolean throwing, buffing, rotating, swapping;

    public AutoBuffModule() {
        addSettings(potions, mode, ticks, autoDisable, onlyOnGround);
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(0, e -> {
            if (mc.player == null || mc.world == null) return;
            if (onlyOnGround.getValue() && !mc.player.isOnGround()) return;
            if (mc.player.age < ticks.getValue()) return;

            if (mode.is("Packet")) handlePacket(); else handleLegit();
        }));
        addEvents(tick);
    }

    @Override
    public void onDisable() {
        if (swapping && potionInvSlot != -1 && tempHotbarSlot != -1) swapBack();
        throwing = buffing = rotating = swapping = false;
        toApply.clear(); toReapply.clear();
    }

    private void handlePacket() {
        if (!shouldBuff() || !throwTimer.finished(250)) return;
        if (rotating && rotationTimer.finished(5)) {
            throwAll(); throwTimer.reset();
            throwing = true; rotating = false;
        } else if (throwing) finish(true);
        else prepareThrow();
    }

    private void handleLegit() {
        long d = 10;
        if (buffing) {
            if (throwing && throwTimer.finished(d)) resetThrow();
            else if (rotating && rotationTimer.finished(d/2)) usePotion();
            else if (toApply.isEmpty() && toReapply.isEmpty()) finish(false);
            else prepareNext();
            return;
        }
        if (shouldBuff()) startBuff();
    }

    private void startBuff() {
        originalPitch = mc.player.getPitch();
        originalSlot = mc.player.getInventory().selectedSlot;
        rotateDown(); prepareList(); buffing = true;
    }

    private void rotateDown() {
        RotationManager.getInstance().addRotation(
                new Rotation(mc.player.getYaw(), 90),
                RotationStrategy.TARGET, TaskPriority.CRITICAL, this
        );
    }

    private void finish(boolean packet) {
        if (swapping && !packet) swapBack();
        if (!packet) InventoryUtil.swapToSlot(originalSlot);
        RotationManager.getInstance().addRotation(
                new Rotation(mc.player.getYaw(), originalPitch),
                RotationStrategy.TARGET, TaskPriority.HIGH, this
        );
        if (!packet) buffing = false; else throwing = false;
        if (autoDisable.getValue() && (packet || allBuffed())) toggle();
    }

    private void resetThrow() {
        throwing = rotating = false;
        if (swapping && potionInvSlot != -1 && tempHotbarSlot != -1) swapBack();
    }

    private void swapBack() {
        Runnable r = () -> InventoryUtil.swapSlots(potionInvSlot, tempHotbarSlot);
        if (SlownessManager.isEnabled()) SlownessManager.applySlowness(10, r); else r.run();
        swapping = false; potionInvSlot = tempHotbarSlot = -1;
    }

    private boolean shouldBuff() {
        return potions.getList().stream().anyMatch(p -> {
            var e = getEffect(p);
            return e != null && !mc.player.hasStatusEffect(e) && hasPotion(e);
        });
    }

    private boolean allBuffed() {
        return potions.getList().stream().allMatch(p -> {
            var e = getEffect(p);
            return e == null || mc.player.hasStatusEffect(e) || !hasPotion(e);
        });
    }

    private void prepareList() {
        toApply.clear(); toReapply.clear();
        for (String p : potions.getList()) {
            var e = getEffect(p);
            if (e != null && !mc.player.hasStatusEffect(e) && hasPotion(e))
                toApply.add(p);
        }
    }

    private RegistryEntry<StatusEffect> getEffect(String p) {
        return switch (p) {
            case "Strength" -> StatusEffects.STRENGTH;
            case "Speed" -> StatusEffects.SPEED;
            case "Fire resistance" -> StatusEffects.FIRE_RESISTANCE;
            default -> null;
        };
    }

    private boolean hasPotion(RegistryEntry<StatusEffect> e) {
        return findSlot(e, 0, 9) != -1 || findSlot(e, 9, 36) != -1;
    }

    private int findSlot(RegistryEntry<StatusEffect> e, int from, int to) {
        for (int i = from; i < to; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() instanceof SplashPotionItem) {
                PotionContentsComponent p = s.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
                boolean match = StreamSupport.stream(p.getEffects().spliterator(), false)
                        .anyMatch(x -> x.getEffectType().equals(e));
                if (match) return i;
            }
        }
        return -1;
    }

    private void prepareNext() {
        String p = !toApply.isEmpty() ? toApply.removeFirst() :
                !toReapply.isEmpty() ? toReapply.removeFirst() : null;
        if (p == null) return;
        var e = getEffect(p); if (e == null) return;

        if (mc.player.hasStatusEffect(e)) return;
        int hot = findSlot(e, 0, 9), inv = findSlot(e, 9, 36);

        if (hot != -1) { InventoryUtil.swapToSlot(hot); prepareThrow(); }
        else if (inv != -1) swapFromInventory(inv, e);
    }

    private void swapFromInventory(int inv, RegistryEntry<StatusEffect> e) {
        swapping = true; potionInvSlot = inv;
        int empty = InventoryUtil.findEmptySlot();
        tempHotbarSlot = (empty != -1 && empty < 9) ? empty : InventoryUtil.findBestSlotInHotBar();

        if (tempHotbarSlot == -1) return;
        Runnable r = () -> {
            InventoryUtil.swapSlots(potionInvSlot, tempHotbarSlot);
            InventoryUtil.swapToSlot(tempHotbarSlot);
            prepareThrow();
        };
        if (SlownessManager.isEnabled()) SlownessManager.applySlowness(10, r); else r.run();
    }

    private void throwAll() {
        int old = mc.player.getInventory().selectedSlot;
        for (String p : potions.getList()) {
            var e = getEffect(p);
            if (e == null || mc.player.hasStatusEffect(e)) continue;
            int hot = findSlot(e, 0, 9), inv = findSlot(e, 9, 36);

            Runnable use = () -> InventoryUtil.useItem(Hand.MAIN_HAND);
            if (hot != -1) { InventoryUtil.swapToSlot(hot); use.run(); }
            else if (inv != -1) {
                int best = InventoryUtil.findBestSlotInHotBar();
                Runnable t = () -> {
                    InventoryUtil.swapSlots(inv, best);
                    InventoryUtil.swapToSlot(best);
                    use.run();
                    InventoryUtil.swapSlots(inv, best);
                };
                if (SlownessManager.isEnabled()) SlownessManager.applySlowness(10, t); else t.run();
            }
        }
        InventoryUtil.swapToSlot(old);
    }

    private void prepareThrow() {
        rotateDown(); rotationTimer.reset(); rotating = true;
    }

    private void usePotion() {
        InventoryUtil.useItem(Hand.MAIN_HAND);
        throwTimer.reset(); throwing = true; rotating = false;
    }
}
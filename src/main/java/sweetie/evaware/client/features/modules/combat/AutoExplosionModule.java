package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.event.events.player.world.BlockPlaceEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.other.SlownessManager;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.rotation.RaytracingUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationMode;
import sweetie.evaware.api.utils.rotation.manager.RotationStrategy;
import sweetie.evaware.api.utils.rotation.rotations.SnapRotation;
import sweetie.evaware.api.utils.task.TaskPriority;
import sweetie.evaware.client.features.modules.movement.MoveFixModule;

import java.util.List;

@ModuleRegister(name = "Auto Explosion", category = Category.COMBAT)
public class AutoExplosionModule extends Module {
    @Getter private static final AutoExplosionModule instance = new AutoExplosionModule();

    private final ModeSetting aimMode = new ModeSetting("Aim mode").value("Instant").values("Instant");
    private final SliderSetting distance = new SliderSetting("Distance").value(3f).range(2.5f, 6f).step(0.1f);
    private final SliderSetting placeDelay = new SliderSetting("Place delay").value(2f).range(0f, 20f).step(1f);
    private final SliderSetting attackDelay = new SliderSetting("Attack delay").value(5f).range(0f, 20f).step(1f);
    private final MultiBooleanSetting options = new MultiBooleanSetting("Options").value(
            new BooleanSetting("Raytrace").value(true),
            new BooleanSetting("No suicide").value(true),
            new BooleanSetting("Delayed swap back").value(false)
    );
    private final SliderSetting swapBackDelay = new SliderSetting("Swap back delay").value(5f).range(1f, 20f).step(1f).setVisible(() -> options.isEnabled("Delayed swap back"));

    public AutoExplosionModule() {
        addSettings(aimMode, distance, placeDelay, attackDelay, options, swapBackDelay);
    }

    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil placeTimer = new TimerUtil();
    private final TimerUtil swapBackTimer = new TimerUtil();
    private final TimerUtil rotationTimer = new TimerUtil();

    private Entity crystalEntity = null;
    private BlockPos obsidianPos = null;

    private int prevSlot = -1;
    private int currentSlot = -1;
    private int bestSlot = -1;

    private boolean swapBack = false;

    private Runnable placeRunnable = null;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEvent() {
        EventListener blockPlaceEvent = BlockPlaceEvent.getInstance().subscribe(new Listener<>(event -> {
            handlePlaceEvent(event);
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handleTickEvent();
        }));

        addEvents(tickEvent, blockPlaceEvent);
    }

    private void handlePlaceEvent(BlockPlaceEvent.BlockPlaceEventData event) {
        if (event.state().getBlock() == Blocks.OBSIDIAN || event.state().getBlock() == Blocks.BEDROCK) {
            obsidianPos = event.pos();
            boolean isOffhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;

            int slotInv = InventoryUtil.findItem(Items.END_CRYSTAL, false);
            int slotHb = InventoryUtil.findItem(Items.END_CRYSTAL, true);
            bestSlot = InventoryUtil.findEmptySlot();

            if (options.isEnabled("Delayed swap back") && mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                swapBackTimer.reset();
            }

            if (isOffhand) {
                if (obsidianPos != null) {
                    placeRunnable = () -> placeCrystal(bestSlot, obsidianPos);
                    placeTimer.reset();
                }
            } else if (slotHb == -1 && slotInv != -1 && bestSlot != -1) {
                placeRunnable = () -> {
                    if (SlownessManager.isEnabled()) {
                        SlownessManager.applySlowness(10, () -> {
                            funnyBabah(slotInv);
                        });
                    } else {
                        funnyBabah(slotInv);
                    }
                };
                placeTimer.reset();
            } else if (slotHb != -1) {
                placeRunnable = () -> {
                    if (obsidianPos == null) {
                        return;
                    }

                    prevSlot = mc.player.getInventory().selectedSlot;
                    placeCrystal(slotHb, obsidianPos);

                    if (options.isEnabled("Delayed swap back")) {
                        swapBackTimer.reset();
                        swapBack = true;
                        currentSlot = mc.player.getInventory().selectedSlot;
                    } else {
                        mc.player.getInventory().selectedSlot = prevSlot;
                    }
                };
                placeTimer.reset();
            }
        }
    }

    private void handleTickEvent() {
        if (crystalEntity != null && !crystalEntity.isAlive()) {
            reset();
        }

        if (placeRunnable != null && placeTimer.finished(placeDelay.getValue() * 50)) {
            placeRunnable.run();
            placeTimer.reset();
            placeRunnable = null;
        }

        if (obsidianPos != null && attackTimer.finished(attackDelay.getValue() * 50)) {
            for (EndCrystalEntity crystal : findCrystals(obsidianPos)) {
                if (isValid(crystal)) {
                    attackCrystal(crystal);
                }
            }
        }

        if (options.isEnabled("Delayed swap back") && swapBack) {
            int playerCurrentSlot = mc.player.getInventory().selectedSlot;

            if (playerCurrentSlot != currentSlot && playerCurrentSlot != prevSlot) {
                swapBack = false;
                return;
            }

            if (swapBackTimer.finished(swapBackDelay.getValue() * 50)) {
                mc.player.getInventory().selectedSlot = prevSlot;
                swapBack = false;
            }
        }
    }

    private void attackCrystal(Entity entity) {
        if (isValid(entity) &&
                mc.player.getAttackCooldownProgress(1f) >= 1f) {

            EntityHitResult hitResult = RaytracingUtil.raytraceEntity(distance.getValue(), rotate(entity), false);

            boolean successRaytarce = hitResult != null && hitResult.getEntity() == entity || !options.isEnabled("Raytrace");

            if (successRaytarce) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                attackTimer.reset();

                crystalEntity = entity;

            }
        }

        if (!entity.isAlive()) {
            crystalEntity = null;
            obsidianPos = null;
        }
    }

    private Rotation rotate(Entity entity) {
        Vec3d targetPos = RotationUtil.getSpot(entity);
        Rotation rotations = RotationUtil.rotationAt(targetPos);

        RotationStrategy configurable = new RotationStrategy(getAimMode(), MoveFixModule.enabled(), MoveFixModule.isFree()).ticksUntilReset(attackDelay.getValue().intValue());

        RotationManager.getInstance().addRotation(rotations, configurable, TaskPriority.REQUIRED, this);

        return rotations;
    }

    private RotationMode getAimMode() {
        return new SnapRotation();
    }

    private void placeCrystal(int slot, BlockPos pos) {
        boolean isOffhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        Vec3d center = Vec3d.ofCenter(pos);
        BlockHitResult hitResult = new BlockHitResult(center, Direction.UP, pos, false);

        if (isOffhand) {
            if (mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hitResult).isAccepted()) {
                mc.player.swingHand(Hand.OFF_HAND);
            }
        } else {
            mc.player.getInventory().selectedSlot = slot;
            if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult).isAccepted() &&
                    mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private boolean isValid(Entity entity) {
        if (entity == null || obsidianPos == null || !entity.isAlive()) {
            return false;
        }

        if (options.isEnabled("No suicide")) {
            if (mc.player.getY() > obsidianPos.getY()) {
                return false;
            }
        }

        return mc.player.getEyePos().distanceTo(RotationUtil.getSpot(entity)) < distance.getValue();
    }

    private List<EndCrystalEntity> findCrystals(BlockPos pos) {
        return mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                new Box(pos).expand(1.0, 2.0, 1.0),
                endCrystalEntity -> endCrystalEntity != null && endCrystalEntity.isAlive()
        );
    }

    private void funnyBabah(int slot) {
        InventoryUtil.swapSlots(slot, bestSlot);
        if (obsidianPos != null) {
            prevSlot = mc.player.getInventory().selectedSlot;
            placeCrystal(bestSlot, obsidianPos);

            if (options.isEnabled("Delayed swap back")) {
                swapBackTimer.reset();
                swapBack = true;
                currentSlot = mc.player.getInventory().selectedSlot;
            } else {
                mc.player.getInventory().selectedSlot = prevSlot;
            }
        }
        InventoryUtil.swapSlots(bestSlot, slot);
    }

    private void reset() {
        crystalEntity = null;
        obsidianPos = null;
        prevSlot = -1;
        bestSlot = -1;
        swapBack = false;
        currentSlot = -1;

        placeTimer.reset();
        attackTimer.reset();
        swapBackTimer.reset();
        rotationTimer.reset();

        placeRunnable = null;
    }
}

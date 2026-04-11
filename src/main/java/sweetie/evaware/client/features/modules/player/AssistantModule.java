package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.event.events.render.Render2DEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BindSetting;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.system.backend.Pair;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.ProjectionUtil;
import sweetie.evaware.api.utils.player.InventoryUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.api.utils.rotation.manager.Rotation;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@ModuleRegister(name = "Assistant", category = Category.PLAYER)
public class AssistantModule extends Module {
    @Getter private static final AssistantModule instance = new AssistantModule();

    public enum Mode {
        FUNTIME,
        HOLYWORLD;

    }

    private final Supplier<Boolean> isHotkeysEnabled = () -> getFunctions().isEnabled("Hotkeys");
    private final Supplier<Boolean> isHWKeys = () -> isHotkeysEnabled.get() && getMode().is("Holy World");
    private final Supplier<Boolean> isFTKeys = () -> isHotkeysEnabled.get() && getMode().is("Fun Time");

    private Mode currentMode = Mode.FUNTIME;

    @Getter private final MultiBooleanSetting functions = new MultiBooleanSetting("Functions").value(
            new BooleanSetting("Hotkeys").value(true),
            new BooleanSetting("Timers").value(false)
    );

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Fun Time")
            .values("Fun Time", "Holy World").setVisible(isHotkeysEnabled)
            .onAction(() -> {
                currentMode = switch (getMode().getValue()) {
                    case "Fun Time" -> Mode.FUNTIME;
                    default -> Mode.HOLYWORLD;
                };
            });

    private final BooleanSetting legit = new BooleanSetting("Legit").value(true).setVisible(isHotkeysEnabled);
    private final Map<InventoryUtil.ItemUsage, Pair<BindSetting, Mode>> keyBindings = new HashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<Pair<Long, Vec3d>> consumables = new ArrayList<>();
    private final Map<Vec3d, String> consumableNames = new HashMap<>();

    public AssistantModule() {
        keyBindings.put(new InventoryUtil.ItemUsage(Items.ENDER_EYE, this), new Pair<>(new BindSetting("Disorientation").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHERITE_SCRAP, this), new Pair<>(new BindSetting("Trap").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SUGAR, this), new Pair<>(new BindSetting("Clear dust").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new Pair<>(new BindSetting("Fire whirl").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.DRIED_KELP, this), new Pair<>(new BindSetting("Plast").value(-999), Mode.FUNTIME));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.PHANTOM_MEMBRANE, this), new Pair<>(new BindSetting("Divine aura").value(-999), Mode.FUNTIME));

        keyBindings.put(new InventoryUtil.ItemUsage(Items.PRISMARINE_SHARD, this), new Pair<>(new BindSetting("Explosive trap").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.POPPED_CHORUS_FRUIT, this), new Pair<>(new BindSetting("Default trap").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHER_STAR, this), new Pair<>(new BindSetting("Stun").value(-999), Mode.HOLYWORLD));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new Pair<>(new BindSetting("Explosive thing").value(-999), Mode.HOLYWORLD));

        addSettings(functions, mode);

        keyBindings.forEach((key, value) -> {
            if (value.right() == Mode.HOLYWORLD) {
                value.left().setVisible(isHWKeys);
            }
            if (value.right() == Mode.FUNTIME) {
                value.left().setVisible(isFTKeys);
            }
            addSettings(value.left());
        });
    }
    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handleTickEvent();
        }));

        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(this::handleRenderEvent));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacketEvent));

        addEvents(tickEvent, renderEvent, packetEvent);
    }

    private void handleRenderEvent(Render2DEvent.Render2DEventData event) {
        if (!functions.isEnabled("Timers")) return;

        MatrixStack matrixStack = event.matrixStack();

        consumables.removeIf(cons -> (double) (cons.left() - System.currentTimeMillis()) <= 0);

        for (Pair<Long, Vec3d> cons : consumables) {
            Vec3d position = cons.right();
            Vector2f screenPos = ProjectionUtil.project(position);

            if (screenPos.x == Float.MAX_VALUE || screenPos.y == Float.MAX_VALUE) continue;

            double time = MathUtil.round((double) (cons.left() - System.currentTimeMillis()) / 1000, 1);
            String name = consumableNames.getOrDefault(position, "Timer");
            String text = name + ": " + time + "s";
            float size = 7f;
            float gap = 3f;

            float textWidth = Fonts.PS_BOLD.getWidth(text, size);
            float posX = screenPos.x - textWidth / 2f;
            float posY = screenPos.y;

            RenderUtil.BLUR_RECT.draw(matrixStack, posX, posY, textWidth + gap * 2f, size + gap * 2f, 2f, UIColors.blur());
            Fonts.PS_BOLD.drawText(matrixStack, text, posX + gap, posY + gap, size, UIColors.textColor());
        }
    }

    private void handlePacketEvent(PacketEvent.PacketEventData event) {
        if (!functions.isEnabled("Timers") || event.isSend()) return;

        if (event.packet() instanceof PlaySoundS2CPacket soundPacket) {
            String soundPath = soundPacket.getSound().getIdAsString();
            if (soundPath.equals("minecraft:block.piston.contract")) {
                Vec3d pos = Vec3d.ofCenter(new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ()));
                consumables.add(new Pair<>(System.currentTimeMillis() + 15000, pos));
                consumableNames.put(pos, "Trap");
            } else if (soundPath.equals("minecraft:block.anvil.place")) {
                BlockPos soundPos = new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ());
                long delay = 250;
                scheduler.schedule(() -> getCube(soundPos, 4, 4).stream().filter(pos -> getDistance(soundPos, pos) > 2 && mc.world.getBlockState(pos).getBlock() == Blocks.COBBLESTONE).min(Comparator.comparing(pos -> getDistance(soundPos, pos))).ifPresent(pos -> {
                    if (getCube(pos, 1, 1).stream().anyMatch(p -> mc.world.getBlockState(p).getBlock() == Blocks.ANVIL)) return;

                    long solidCount = getCube(pos, 1, 1).stream().filter(p -> {
                        BlockState s = mc.world.getBlockState(p);return !s.isAir() && s.isSolidBlock(mc.world, p);}).count();
                    print(String.valueOf(solidCount));
                    if (solidCount == 18 || solidCount == 15 || solidCount == 5) {
                        int time = solidCount == 18 || solidCount == 15 ? 20000 : 15000;
                        Vec3d addPos = Vec3d.ofCenter(pos).add(0, solidCount == 5 ? -1.5 : 0, 0);
                        consumables.add(new Pair<>(System.currentTimeMillis() + time - delay, addPos));
                        consumableNames.put(addPos, solidCount == 18 || solidCount == 15 ? "Plast" : "Trap");
                    }
                }), delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void handleTickEvent() {
        if (!isHotkeysEnabled.get() || mc.currentScreen != null) return;

        keyBindings.forEach((key, value) -> {
            if (value.right() == currentMode) {
                key.handleUse(value.left().getValue(), legit.getValue());
            }
        });
    }

    private double getDistance(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<BlockPos> getCube(BlockPos center, int xRadius, int yRadius) {
        List<BlockPos> sphere = new ArrayList<>();
        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    sphere.add(center.add(x, y, z));
                }
            }
        }
        return sphere;
    }
}

package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.other.TextUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleRegister(name = "Use Tracker", category = Category.OTHER)
public class UseTrackerModule extends Module {
    @Getter private static final UseTrackerModule instance = new UseTrackerModule();

    private final BooleanSetting trackTotem = new BooleanSetting("Track Totems").value(true);
    private final BooleanSetting trackPotions = new BooleanSetting("Track Potions").value(true);
    private final BooleanSetting trackConsume = new BooleanSetting("Track Consume").value(true);
    private final SliderSetting radius = new SliderSetting("Potion Radius").value(100f).range(10f, 100f).step(1f);

    private static class PotionData {
        ItemStack stack;
        double lastX, lastY, lastZ;
        
        PotionData(ItemStack stack, double x, double y, double z) {
            this.stack = stack;
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
        }
    }

    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
    private final Map<UUID, Integer> useStartTick = new HashMap<>();

    @Override
    public void onDisable() {
        trackedPotions.clear();
        activeUseItem.clear();
        useStartTick.clear();
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            if (trackConsume.getValue()) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player.getUuid().equals(mc.player.getUuid())) continue;
                    
                    UUID id = player.getUuid();
                    
                    if (player.isUsingItem()) {
                        if (!activeUseItem.containsKey(id)) {
                            activeUseItem.put(id, player.getActiveItem().copy());
                            useStartTick.put(id, player.age);
                        }
                    } else {
                        ItemStack used = activeUseItem.remove(id);
                        Integer startTick = useStartTick.remove(id);
                        
                        if (used == null || used.isEmpty() || startTick == null) continue;
                        if (player.age - startTick < 31) continue;
                        
                        UseAction action = used.getUseAction();
                        String verb = switch (action) {
                            case DRINK -> "выпил";
                            case EAT -> "съел";
                            default -> null;
                        };
                        
                        if (verb == null) continue;
                        
                        String name = used.getName().getString();
                        print(player.getName().getString() + " " + verb + " " + name);
                    }
                }
            }

            if (trackPotions.getValue()) {
                Set<Integer> current = new HashSet<>();
                float r = radius.getValue();

                for (var entity : mc.world.getEntities()) {
                    if (!(entity instanceof PotionEntity potion)) continue;
                    
                    double dist = mc.player.distanceTo(potion);
                    if (dist > r) continue;

                    int eid = potion.getId();
                    current.add(eid);
                    
                    if (!trackedPotions.containsKey(eid)) {
                        ItemStack stack = potion.getStack();
                        trackedPotions.put(eid, new PotionData(stack.copy(), potion.getX(), potion.getY(), potion.getZ()));
                    } else {
                        PotionData d = trackedPotions.get(eid);
                        d.lastX = potion.getX();
                        d.lastY = potion.getY();
                        d.lastZ = potion.getZ();
                    }
                }

                Set<Integer> removed = new HashSet<>(trackedPotions.keySet());
                removed.removeAll(current);

                for (int eid : removed) {
                    PotionData data = trackedPotions.remove(eid);
                    if (data == null) continue;

                    Box hitBox = new Box(
                        data.lastX - 4, data.lastY - 2, data.lastZ - 4,
                        data.lastX + 4, data.lastY + 2, data.lastZ + 4
                    );

                    for (LivingEntity hit : mc.world.getEntitiesByClass(LivingEntity.class, hitBox, e2 -> true)) {
                        if (!(hit instanceof PlayerEntity player)) continue;
                        
                        double dx = player.getX() - data.lastX;
                        double dz = player.getZ() - data.lastZ;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 4.0) continue;

                        String potionName = data.stack.getName().getString();
                        String playerName = player.getName().getString();
                        double hitChance = Math.max(0, 1.0 - dist / 4.0) * 100.0;

                        Formatting hitColor = hitChance >= 65 ? Formatting.GREEN
                                : hitChance >= 35 ? Formatting.YELLOW
                                : Formatting.RED;

                        Text message = Text.empty()
                            .append(TextUtil.gradient("Woqued", true))
                            .append(Text.literal(" » ").styled(s -> s.withColor(Formatting.GRAY)))
                            .append(Text.literal(playerName).styled(s -> s.withColor(Formatting.WHITE)))
                            .append(Text.literal(" получил ").styled(s -> s.withColor(Formatting.GRAY)))
                            .append(Text.literal(potionName))
                            .append(Text.literal(" (").styled(s -> s.withColor(Formatting.GRAY)))
                            .append(Text.literal(String.format("%.0f%%", hitChance)).styled(s -> s.withColor(hitColor)))
                            .append(Text.literal(")").styled(s -> s.withColor(Formatting.GRAY)));

                        mc.player.sendMessage(message, false);
                    }
                }
            }
        }));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive()) return;
            if (mc.world == null || mc.player == null) return;

            if (trackTotem.getValue() && event.packet() instanceof EntityStatusS2CPacket pkt && pkt.getStatus() == 35) {
                var entity = pkt.getEntity(mc.world);
                if (!(entity instanceof PlayerEntity player)) return;
                if (player.getUuid().equals(mc.player.getUuid())) return;

                ItemStack offHand = player.getOffHandStack();
                ItemStack mainHand = player.getMainHandStack();
                
                boolean hasTotem = offHand.getItem().equals(net.minecraft.item.Items.TOTEM_OF_UNDYING) 
                                || mainHand.getItem().equals(net.minecraft.item.Items.TOTEM_OF_UNDYING);

                String totemName = hasTotem 
                        ? (offHand.getItem().equals(net.minecraft.item.Items.TOTEM_OF_UNDYING) 
                            ? offHand.getName().getString() 
                            : mainHand.getName().getString())
                        : "Тотем бессмертия";

                boolean isEnchanted = !offHand.isEmpty() && offHand.hasEnchantments() 
                                || !mainHand.isEmpty() && mainHand.hasEnchantments();

                Formatting enchantColor = isEnchanted ? Formatting.GREEN : Formatting.RED;

                Text message = Text.empty()
                    .append(TextUtil.gradient("Woqued", true))
                    .append(Text.literal(" » ").styled(s -> s.withColor(Formatting.GRAY)))
                    .append(Text.literal(player.getName().getString()).styled(s -> s.withColor(Formatting.WHITE)))
                    .append(Text.literal(" потерял ").styled(s -> s.withColor(Formatting.GRAY)))
                    .append(Text.literal(totemName))
                    .append(Text.literal(" (").styled(s -> s.withColor(Formatting.GRAY)))
                    .append(Text.literal(isEnchanted ? "зачарованный" : "незачарованный").styled(s -> s.withColor(enchantColor)))
                    .append(Text.literal(")").styled(s -> s.withColor(Formatting.GRAY)));

                mc.player.sendMessage(message, false);
            }
        }));

        addEvents(tickEvent, packetEvent);
    }
}

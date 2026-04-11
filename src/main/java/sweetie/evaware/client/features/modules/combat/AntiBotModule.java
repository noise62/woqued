package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;

import java.util.*;

@ModuleRegister(name = "Anti Bot", category = Category.COMBAT)
public class AntiBotModule extends Module {
    @Getter private static final AntiBotModule instance = new AntiBotModule();

    private final ModeSetting mode = new ModeSetting("Mode")
            .value("ReallyWorld").values("Matrix", "ReallyWorld");

    private final Set<UUID> suspectSet = new HashSet<>();
    private final Set<UUID> botSet = new HashSet<>();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public AntiBotModule() {
        addSettings(mode);
    }

    @Override
    public void onEvent() {
        // Исправленная логика обработки пакетов
        EventListener packetListener = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            // В твоем клиенте 'event' — это уже PacketEventData
            if (mc.world == null || !event.isReceive()) return;

            var packet = event.packet(); // Получаем пакет напрямую из event

            if (packet instanceof PlayerListS2CPacket list) {
                checkPlayerAfterSpawn(list);
            } else if (packet instanceof PlayerRemoveS2CPacket remove) {
                removePlayerBecauseLeftServer(remove);
            }
        }));

        // Логика проверки каждый тик (здесь все должно быть ок, если UpdateEvent работает так же)
        EventListener updateListener = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            if (!suspectSet.isEmpty()) {
                mc.world.getPlayers().stream()
                        .filter(p -> suspectSet.contains(p.getUuid()))
                        .forEach(this::evaluateSuspectPlayer);
            }

            if (mode.is("Matrix")) {
                matrixMode();
            } else if (mode.is("ReallyWorld")) {
                reallyWorldMode();
            }
        }));

        addEvents(packetListener, updateListener);
    }

    private void checkPlayerAfterSpawn(PlayerListS2CPacket listS2CPacket) {
        listS2CPacket.getPlayerAdditionEntries().forEach(entry -> {
            var profile = entry.profile();
            if (profile == null || isRealPlayer(entry)) return;

            // Используем getId() вместо id()
            if (isDuplicateProfile(profile.getName(), profile.getId())) {
                botSet.add(profile.getId());
            } else {
                suspectSet.add(profile.getId());
            }
        });
    }

    private void removePlayerBecauseLeftServer(PlayerRemoveS2CPacket removeS2CPacket) {
        removeS2CPacket.profileIds().forEach(uuid -> {
            suspectSet.remove(uuid);
            botSet.remove(uuid);
        });
    }

    private boolean isRealPlayer(PlayerListS2CPacket.Entry entry) {
        // Заменяем properties() на getProperties()
        return entry.latency() < 2 || (entry.profile().getProperties() != null && !entry.profile().getProperties().isEmpty());
    }

    private void evaluateSuspectPlayer(PlayerEntity player) {
        if (isFullyEquipped(player)) {
            botSet.add(player.getUuid());
        }
        suspectSet.remove(player.getUuid());
    }

    private void matrixMode() {
        Iterator<UUID> iterator = suspectSet.iterator();
        while (iterator.hasNext()) {
            UUID susPlayer = iterator.next();
            PlayerEntity entity = mc.world.getPlayerByUuid(susPlayer);
            if (entity != null) {
                String name = entity.getName().getString();
                boolean isNameBot = name.startsWith("CIT-") && !name.contains("NPC");
                int armorCount = 0;
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    if (!entity.getEquippedStack(slot).isEmpty()) armorCount++;
                }
                boolean isFakeUUID = !entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()));
                if (armorCount == 4 || isNameBot || isFakeUUID) {
                    botSet.add(susPlayer);
                }
            }
            iterator.remove();
        }
        if (mc.player.age % 100 == 0) {
            botSet.removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
        }
    }

    private void reallyWorldMode() {
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player) continue;
            String name = entity.getName().getString();
            UUID expectedUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
            if (!entity.getUuid().equals(expectedUuid) && !botSet.contains(entity.getUuid())
                    && !name.contains("NPC") && !name.startsWith("[ZNPC]")) {
                botSet.add(entity.getUuid());
            }
        }
    }

    private boolean isDuplicateProfile(String name, UUID id) {
        if (mc.getNetworkHandler() == null) return false;
        return mc.getNetworkHandler().getPlayerList().stream()
                .anyMatch(p -> p.getProfile().getName().equals(name) && !p.getProfile().getId().equals(id));
    }

    private boolean isFullyEquipped(PlayerEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (stack.isEmpty()) return false;
            EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
            if (equippable == null) return false;
        }
        return true;
    }

    public boolean isBot(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        String name = entity.getName().getString();
        if (name.startsWith("CIT-") && !name.contains("NPC")) return true;
        boolean isInvalidUUID = !entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()));
        if (isInvalidUUID && entity.isInvisible() && !name.contains("NPC")) return true;
        return botSet.contains(entity.getUuid());
    }

    @Override
    public void onEnable() {
        suspectSet.clear();
        botSet.clear();
    }

    @Override
    public void onDisable() {
        suspectSet.clear();
        botSet.clear();
    }
}
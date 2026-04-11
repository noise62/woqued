package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ModuleRegister(name = "Auto Duel", category = Category.OTHER)
public class AutoDuelModule extends Module {
    @Getter private static final AutoDuelModule instance = new AutoDuelModule();

    // Настройки в стиле твоего чита
    private final ModeSetting priority = new ModeSetting("Priority")
            .value("Random").values("Random", "Softers", "Unsofters");

    private final ModeSetting kit = new ModeSetting("Kit")
            .value("Щит").values("Щит", "Шипы 3", "Лук", "Тотем", "НоуДебаф", "Шары", "Классик", "Читерский рай", "Незер");

    private final List<String> sentPlayers = new ArrayList<>();
    private long lastDuelTime = 0L;

    public AutoDuelModule() {
        // Регистрация настроек через метод, который я увидел в твоей Ауре
        addSettings(priority, kit);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.player.networkHandler == null) return;

            // 1. Логика отправки запросов на дуэль
            List<String> playerNames = new ArrayList<>();
            for (PlayerListEntry entry : mc.player.networkHandler.getPlayerList()) {
                playerNames.add(entry.getProfile().getName());
            }

            // Обработка приоритета
            if (priority.is("Random")) {
                Collections.shuffle(playerNames);
            } else if (priority.is("Softers")) {
                Collections.reverse(playerNames);
            }

            // Проверка задержки и отправка команды
            for (String name : playerNames) {
                if (System.currentTimeMillis() - lastDuelTime > 750L
                        && !sentPlayers.contains(name)
                        && !name.equals(mc.player.getName().getString())) {

                    mc.player.networkHandler.sendChatCommand("duel " + name);
                    sentPlayers.add(name);
                    lastDuelTime = System.currentTimeMillis();
                    break; // Отправляем один запрос за один проход
                }
            }

            // 2. Автоматическое нажатие в меню (GUI)
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
                if (mc.currentScreen == null) return;

                String title = mc.currentScreen.getTitle().getString();

                if (title.contains("Выбор набора")) {
                    int slotIndex = getKitSlot(kit.getValue());
                    if (slotIndex != -1) {
                        click(container.syncId, slotIndex);
                    }
                } else if (title.contains("Настройка поединка")) {
                    click(container.syncId, 0); // Обычно слот 0 — это подтверждение
                }
            }
        }));

        // Регистрация события через метод твоего базового класса
        addEvents(updateEvent);
    }

    private void click(int syncId, int slot) {
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.clickSlot(
                    syncId,
                    slot, 0,
                    SlotActionType.PICKUP,
                    mc.player
            );
        }
    }

    private int getKitSlot(String kitName) {
        return switch (kitName) {
            case "Щит" -> 0;
            case "Шипы 3" -> 1;
            case "Лук" -> 2;
            case "Тотем" -> 3;
            case "НоуДебаф" -> 4;
            case "Шары" -> 5;
            case "Классик" -> 6;
            case "Читерский рай" -> 7;
            case "Незер" -> 8;
            default -> -1;
        };
    }

    @Override
    public void onEnable() {
        sentPlayers.clear();
        lastDuelTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        sentPlayers.clear();
    }
}
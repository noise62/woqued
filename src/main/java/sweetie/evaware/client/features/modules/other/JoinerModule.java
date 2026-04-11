package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.utils.player.InventoryUtil;

@ModuleRegister(name = "Joiner", category = Category.OTHER)
public class JoinerModule extends Module {
    @Getter private static final JoinerModule instance = new JoinerModule();

    private boolean compassClick = false;
    private long last;
    private boolean restart;

    @Override
    public void toggle() {
        super.toggle();
        compassClick = false;
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if ((!compassClick || restart) && mc.currentScreen == null) {
                int compassSlot = InventoryUtil.findItem(Items.COMPASS, true);
                if (compassSlot == -1) return;

                InventoryUtil.swapToSlot(compassSlot);
                InventoryUtil.useItem(Hand.MAIN_HAND);
                compassClick = true;
            }


            if (compassClick && mc.currentScreen instanceof GenericContainerScreen screen) {
                for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                    ItemStack stack = screen.getScreenHandler().getSlot(i).getStack();
                    if (stack.getName().getString().contains("Дуэли")) {
                        InventoryUtil.swapSlots(i, 4);
                        compassClick = false;
                        restart = false;
                        break;
                    }
                }
            }
        }));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive()) return;

            if (event.packet() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();
                if (message.contains("Вы уже подключены на этот сервер")
                        || message.contains("Сервер заполнен")
                        || message.contains("Вы были кикнуты с сервера 1duels")) {
                    compassClick = false;
                    restart = true;
                    PacketEvent.getInstance().setCancel(true);
                }
            }
        }));

        addEvents(updateEvent, packetEvent);
    }
}

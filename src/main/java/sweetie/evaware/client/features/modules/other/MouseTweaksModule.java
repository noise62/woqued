package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.world.ClickSlotEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.system.backend.KeyStorage;

@ModuleRegister(name = "Mouse Tweaks", category = Category.OTHER)
public class MouseTweaksModule extends Module {
    @Getter private static final MouseTweaksModule instance = new MouseTweaksModule();

    public final SliderSetting delay = new SliderSetting("Delay").value(0f).range(0f, 200f).step(5f);
    private boolean stop = false;

    public MouseTweaksModule() {
        addSettings(delay);
    }

    @Override
    public void onEvent() {
        EventListener clickSlotEvent = ClickSlotEvent.getInstance().subscribe(new Listener<>(event -> {
            if ((KeyStorage.isPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || KeyStorage.isPressed(GLFW.GLFW_KEY_RIGHT_SHIFT))
                    && (KeyStorage.isPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyStorage.isPressed(GLFW.GLFW_KEY_RIGHT_CONTROL))
                    && event.slotActionType() == SlotActionType.THROW
                    && !stop) {
                Item copy = mc.player.currentScreenHandler.slots.get(event.slot()).getStack().getItem();
                stop = true;
                for (int i2 = 0; i2 < mc.player.currentScreenHandler.slots.size(); ++i2) {
                    if (mc.player.currentScreenHandler.slots.get(i2).getStack().getItem() == copy)
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i2, 1, SlotActionType.THROW, mc.player);
                }
                stop = false;
            }
        }));

        addEvents(clickSlotEvent);
    }
}

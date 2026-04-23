package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.SliderSetting;

@ModuleRegister(name = "Auto GApple", category = Category.COMBAT)
public class AutoGAppleModule extends Module {
    @Getter private static final AutoGAppleModule instance = new AutoGAppleModule();
    private final SliderSetting health = new SliderSetting("Health").value(18f).range(4f, 20f).step(1f);
    private final BooleanSetting useEnchanted= new BooleanSetting("Use enchanted").value(true);

    private boolean active;

    public AutoGAppleModule() {
        addSettings(health, useEnchanted);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            boolean validItem = mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE || useEnchanted.getValue() && mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE;

            if (validItem && mc.player.getHealth() <= health.getValue()) {
                active = true;
                if (!mc.player.isUsingItem()) {
                    mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                    KeyBinding.setKeyPressed(mc.options.useKey.getDefaultKey(), true);
                    mc.player.setCurrentHand(Hand.OFF_HAND);
                }
            } else if (active && mc.player.isUsingItem()) {
                mc.interactionManager.stopUsingItem(mc.player);
                if (!(mc.mouse.wasRightButtonClicked() && mc.currentScreen == null)) {
                    KeyBinding.setKeyPressed(mc.options.useKey.getDefaultKey(), false);
                }
                active = false;
            }
        }));

        addEvents(updateEvent);
    }
}

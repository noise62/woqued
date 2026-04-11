package sweetie.evaware.client.features.modules.other;

import lombok.Getter;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.module.setting.SliderSetting;
import sweetie.evaware.api.utils.math.TimerUtil;

import java.util.function.Supplier;

@ModuleRegister(name = "Tape Mouse", category = Category.OTHER)
public class TapeMouseModule extends Module {
    @Getter private static final TapeMouseModule instance = new TapeMouseModule();

    private final MultiBooleanSetting actions = new MultiBooleanSetting("Actions").value(
            new BooleanSetting("Attack").value(true),
            new BooleanSetting("Use").value(false)
    );
    private final Supplier<Boolean> isAttack = () -> actions.isEnabled("Attack");
    private final Supplier<Boolean> isUse = () -> actions.isEnabled("Use");

    private final SliderSetting attackDelay = new SliderSetting("Attack delay").value(10f).range(1f, 20f).step(1f).setVisible(isAttack);
    private final SliderSetting useDelay = new SliderSetting("Use delay").value(10f).range(1f, 20f).step(1f).setVisible(isUse);

    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil useTimer = new TimerUtil();

    public TapeMouseModule() {
        addSettings(actions, attackDelay, useDelay);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (isAttack.get()) handleAction(attackDelay.getValue(), attackTimer, () -> mc.doAttack());
            if (isUse.get()) handleAction(useDelay.getValue(), useTimer, () -> mc.doItemUse());
        }));

        addEvents(tickEvent);
    }

    private void handleAction(float delay, TimerUtil timer, Runnable run) {
        if (timer.finished(delay * 50)) {
            run.run();
            timer.reset();
        }
    }
}

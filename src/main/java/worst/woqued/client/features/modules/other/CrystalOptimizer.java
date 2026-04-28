package worst.woqued.client.features.modules.other;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.player.world.AttackEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

@ModuleRegister(name = "Crystal Optimizer", category = Category.OTHER)
public class CrystalOptimizer extends Module {

    private static CrystalOptimizer instance;

    public static CrystalOptimizer getInstance() {
        if (instance == null) instance = new CrystalOptimizer();
        return instance;
    }

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.entity() instanceof EndCrystalEntity entity) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }));

        addEvents(attackEvent);
    }
}
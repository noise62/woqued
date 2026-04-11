package sweetie.evaware.api.utils.combat;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.LivingEntity;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.MultiBooleanSetting;
import sweetie.evaware.api.utils.rotation.manager.Rotation;

import java.util.List;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatExecutor {
    private final CombatManager combatManager = new CombatManager();
    private final MultiBooleanSetting options = new MultiBooleanSetting("Options").value(
            new BooleanSetting("Only crits").value(true),
            new BooleanSetting("Smart crits").value(true).setVisible(() -> options().isEnabled("Only crits")),
            new BooleanSetting("Raytrace").value(true),
            new BooleanSetting("Shield break").value(true),
            new BooleanSetting("Always shield").value(false),
            new BooleanSetting("Ignore walls").value(false),
            new BooleanSetting("No attack if eat").value(false)
    );

    public void performAttack() {
        combatManager.handleAttack();
    }

    public static class CombatConfigurable {
        public final LivingEntity target;
        public final Rotation rotation;
        public final float distance;

        public final boolean onlyCrits;
        public final boolean smartCrits;
        public final boolean raytrace;
        public final boolean shieldBreak;
        public final boolean alwaysShield;
        public final boolean ignoreWalls;
        public final boolean noAttackIfEat;

        public CombatConfigurable(
                LivingEntity target,
                Rotation rotation,
                float distance,
                boolean onlyCrits,
                boolean smartCrits,
                boolean raytrace,
                boolean shieldBreak,
                boolean alwaysShield,
                boolean ignoreWalls,
                boolean noAttackIfEat
        ) {
            this.target = target;
            this.rotation = rotation;
            this.distance = distance;
            this.onlyCrits = onlyCrits;
            this.smartCrits = smartCrits;
            this.raytrace = raytrace;
            this.shieldBreak = shieldBreak;
            this.alwaysShield = alwaysShield;
            this.ignoreWalls = ignoreWalls;
            this.noAttackIfEat = noAttackIfEat;
        }

        public CombatConfigurable(
                LivingEntity target,
                Rotation rotation,
                float distance,
                List<String> options
        ) {
            this(
                    target,
                    rotation,
                    distance,
                    options.contains("Only crits"),
                    options.contains("Smart crits"),
                    options.contains("Raytrace"),
                    options.contains("Shield break"),
                    options.contains("Always shield"),
                    options.contains("Ignore walls"),
                    options.contains("No attack if eat")
            );
        }
    }
}

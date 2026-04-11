package sweetie.evaware.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.move.SprintEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;
import sweetie.evaware.api.utils.rotation.manager.RotationPlan;
import sweetie.evaware.client.features.modules.combat.AuraModule;

@ModuleRegister(name = "Sprint", category = Category.MOVEMENT)
public class SprintModule extends Module {
    @Getter private static final SprintModule instance = new SprintModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("Legit").values("Legit", "Packet", "None");

    public SprintModule() {
        addSettings(mode);
        setEnabled(true);
    }

    @Override
    public void onEvent() {
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(1, event -> {
            if (!isSprintAllowed()) {
                event.setSprint(true);
            }
        }));

        addEvents(sprintEvent);
    }

    public boolean isSprintAllowed() {
        AuraModule auraModule = AuraModule.getInstance();
        boolean auraCheck = mode.is("Legit") && auraModule.target != null && auraModule.isEnabled() && auraModule.combatExecutor.combatManager().clickScheduler().isOneTickBeforeAttack();

        return (!mc.options.sprintKey.isPressed() || mc.player.input.movementForward != 0) && (hasForwardMovement() || auraCheck);
    }

    @SuppressWarnings("MagicNumber")
    public boolean hasForwardMovement() {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan plan = rotationManager.getCurrentRotationPlan();
        if (plan == null || plan.provider() instanceof StrafeModule || plan.moveCorrection() || mode.is("None")) {
            return false;
        }

        Rotation currentRotation = rotationManager.getCurrentRotation() != null ? rotationManager.getCurrentRotation() : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float deltaYaw = mc.player.getYaw() - currentRotation.getYaw();
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;

        boolean hasForwardMovement = forward * MathHelper.cos(deltaYaw * 0.017453292f) + sideways * MathHelper.sin(deltaYaw * 0.017453292f) > 1.0E-5f;

        return !hasForwardMovement;
    }

}

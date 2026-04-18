package sweetie.evaware.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.events.player.other.UpdateEvent;
import sweetie.evaware.api.event.events.player.world.AttackEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;
import sweetie.evaware.api.module.setting.BooleanSetting;
import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.api.module.setting.SliderSetting;

@ModuleRegister(name = "Super Knockback", category = Category.COMBAT)
public class SuperKnockbackModule extends Module {
    @Getter private static final SuperKnockbackModule instance = new SuperKnockbackModule();

    private final ModeSetting mode = new ModeSetting("Mode").value("Packet").values("Packet", "SprintTap", "WTap");
    private final SliderSetting hurtTime = new SliderSetting("HurtTime").value(10f).range(0f, 10f).step(1f);
    private final SliderSetting chance = new SliderSetting("Chance").value(100f).range(0f, 100f).step(1f);
    private final BooleanSetting onlyOnMove = new BooleanSetting("OnlyOnMove").value(true);

    private int waitTicks = 0;
    private boolean cancelMovement = false;

    public SuperKnockbackModule() {
        addSettings(mode, hurtTime, chance, onlyOnMove);
    }

    @Override
    public void onEvent() {
        EventListener attackListener = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!(event.entity() instanceof LivingEntity target)) return;
            if (!shouldOperate(target)) return;

            if (Math.random() * 100 > chance.getValue()) return;

            switch (mode.getValue()) {
                case "Packet" -> {
                    if (target.hurtTime <= hurtTime.getValue()) {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                    }
                }
                case "SprintTap" -> {
                    if (mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                        waitTicks = 1;
                    }
                }
                case "WTap" -> {
                    if (mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                        cancelMovement = true;
                        waitTicks = 2;
                    }
                }
            }
        }));

        EventListener updateListener = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (waitTicks > 0) {
                waitTicks--;
                if (waitTicks == 0) {
                    if (mode.is("SprintTap")) {
                        mc.player.setSprinting(true);
                    } else if (mode.is("WTap")) {
                        cancelMovement = false;
                        mc.player.setSprinting(true);
                    }
                }
            }
        }));

        addEvents(attackListener, updateListener);
    }

    private boolean shouldOperate(LivingEntity target) {
        if (onlyOnMove.getValue() && mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) {
            return false;
        }
        return true;
    }

    public boolean isCancelingMovement() {
        return cancelMovement;
    }
}

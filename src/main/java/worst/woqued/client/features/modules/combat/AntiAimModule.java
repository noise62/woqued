package worst.woqued.client.features.modules.combat;

import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.events.player.other.UpdateEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.SliderSetting;

@ModuleRegister(name = "Anti Aim", category = Category.COMBAT)
@Environment(EnvType.CLIENT)
public class AntiAimModule extends Module {
    @Getter private static final AntiAimModule instance = new AntiAimModule();

    private final BooleanSetting realBoolean = new BooleanSetting("Change hitbox").value(false);
    private final BooleanSetting randomReal = new BooleanSetting("Anti-Bruteforce").value(true);
    private final BooleanSetting fakeBoolean = new BooleanSetting("Visual AA").value(true);
    private final ModeSetting fakeModeYaw = new ModeSetting("Change Yaw").value("Jitter").values("Jitter", "Static", "Random", "Defense");
    private final SliderSetting yawSlider = new SliderSetting("Yaw Angle").value(60.0f).range(1.0f, 70.0f).step(1.0f);
    private final ModeSetting fakeModePitch = new ModeSetting("Change Pitch").value("Defense").values("Defense", "Custom");
    private final SliderSetting pitchSlider = new SliderSetting("Pitch Angle").value(65.0f).range(0.0f, 90.0f).step(1.0f);
    private final BooleanSetting zeroPitch = new BooleanSetting("Zero pitch on land").value(false);
    private final BooleanSetting chivoBlyat = new BooleanSetting("Show for all").value(false);

    private float yaw = 0;
    private float pitch = 0;
    private long timeLanded = 0;
    private final int delayTime = 500;
    private boolean can = true;

    public AntiAimModule() {
        addSettings(realBoolean, randomReal, fakeBoolean, fakeModeYaw, fakeModePitch, zeroPitch, yawSlider, pitchSlider, chivoBlyat);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(this::onUpdate));
        addEvents(updateEvent);
    }

    private void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        // Сохраняем текущий pitch камеры для восстановления
        float cameraPitch = mc.player.getPitch();

        if (fakeBoolean.getValue()) {
            if (mc.options.useKey.isPressed() || mc.options.attackKey.isPressed() || mc.currentScreen != null) {
                can = false;
                return;
            } else {
                can = true;
            }

            // yaw
            if (fakeModeYaw.getValue().equals("Jitter")) {
                if (mc.player.age % 2 == 0) {
                    yaw = mc.player.getYaw() + yawSlider.getValue() + 180;
                } else {
                    yaw = mc.player.getYaw() - yawSlider.getValue() + 180;
                }
            } else if (fakeModeYaw.getValue().equals("Static")) {
                yaw = mc.player.getYaw() + 180;
            } else if (fakeModeYaw.getValue().equals("Defense")) {
                if (mc.player.age % (int) randomizeFloat(2, 6) == 0) {
                    yaw = mc.player.getYaw() + (int) randomizeFloat(12, 60) + 200;
                } else {
                    yaw = mc.player.getYaw() - (int) randomizeFloat(12, 60) + 200;
                }
            } else if (fakeModeYaw.getValue().equals("Random")) {
                int i = (int) randomizeFloat(1, 180);
                if ((int) randomizeFloat(1, 2) == 1) {
                    yaw = mc.player.getYaw() + 180 + i;
                } else {
                    yaw = mc.player.getYaw() + 180 - i;
                }
            }

            // pitch для анти-аима
            if (fakeModePitch.getValue().equals("Custom")) {
                pitch = pitchSlider.getValue();
            } else if (fakeModePitch.getValue().equals("Defense")) {
                pitch = pitchSlider.getValue();
                if (mc.player.age % (int) randomizeFloat(4, 12) == 0) {
                    pitch = -65;
                }
            }

            if (zeroPitch.getValue()) {
                if (mc.player.isOnGround()) {
                    if (timeLanded == 0) {
                        timeLanded = System.currentTimeMillis();
                    }
                    if (System.currentTimeMillis() - timeLanded <= delayTime) {
                        pitch = 0;
                    }
                } else {
                    timeLanded = 0;
                }
            }

            // Применяем yaw к телу (для отрисовки anti-aim)
            mc.player.bodyYaw = yaw;
            mc.player.setHeadYaw(yaw);

            // Свободная камера: всегда сохраняем pitch от ввода игрока
            // и применяем yaw только если включен chivoBlyat
            if (chivoBlyat.getValue() && can) {
                mc.player.setYaw(yaw);
            }
            // Pitch всегда остаётся от камеры — свобода управления
        }

        if (realBoolean.getValue()) {
            if (mc.player.isSubmergedInWater() || mc.options.useKey.isPressed() || mc.options.attackKey.isPressed() || mc.currentScreen != null) {
                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
                return;
            }

            int i = 4;

            if (randomReal.getValue()) {
                i = (int) randomizeFloat(4, 8);
            }

            if (mc.player.age % i == 0) {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
            } else {
                mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
            }
        }
    }

    private float randomizeFloat(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    private void reset() {
        if (mc.player != null) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }
}

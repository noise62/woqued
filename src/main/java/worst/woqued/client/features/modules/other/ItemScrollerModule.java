package worst.woqued.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.math.TimerUtil;

import java.lang.reflect.Method;

@ModuleRegister(name = "Item Scroller", category = Category.OTHER)
public class ItemScrollerModule extends Module {
    @Getter private static final ItemScrollerModule instance = new ItemScrollerModule();

    private final SliderSetting delay = new SliderSetting("Delay (ms)").value(50f).range(0f, 200f).step(1f);
    private final TimerUtil time = new TimerUtil();

    public ItemScrollerModule() {
        addSettings(delay);
        time.reset();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player != null && mc.currentScreen != null) {
                if (mc.currentScreen instanceof HandledScreen<?> screen) {
                    if (mc.getWindow() != null) {
                        long windowHandle = mc.getWindow().getHandle();
                        boolean isShiftPressed = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == 1
                                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == 1;
                        boolean isLeftMousePressed = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == 1;

                        if (isShiftPressed && isLeftMousePressed) {
                            double[] mouseX = new double[1];
                            double[] mouseY = new double[1];
                            GLFW.glfwGetCursorPos(windowHandle, mouseX, mouseY);

                            // Scale mouse coordinates to GUI coordinates
                            double scaledX = mouseX[0] * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
                            double scaledY = mouseY[0] * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

                            try {
                                Method getSlotAtMethod = HandledScreen.class.getDeclaredMethod("getSlotAt", double.class, double.class);
                                getSlotAtMethod.setAccessible(true);
                                Slot slot = (Slot) getSlotAtMethod.invoke(screen, scaledX, scaledY);

                                if (slot != null && slot.hasStack() && time.finished(delay.getValue().longValue())) {
                                    mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                    time.reset();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }));

        addEvents(tickEvent);
    }
}

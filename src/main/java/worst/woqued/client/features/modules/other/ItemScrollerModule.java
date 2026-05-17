package worst.woqued.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.module.setting.SliderSetting;
import worst.woqued.api.utils.math.TimerUtil;

@ModuleRegister(name = "Item Scroller", category = Category.OTHER)
public class ItemScrollerModule extends Module {
    @Getter private static final ItemScrollerModule instance = new ItemScrollerModule();

    public final SliderSetting delay = new SliderSetting("Delay (ms)").value(0f).range(0f, 200f).step(5f);
    private final TimerUtil timerUtil = new TimerUtil();

    public ItemScrollerModule() {
        addSettings(delay);
        timerUtil.reset();
    }

    @Override
    public void onEvent() {
    }

    public boolean timerFinished() {
        return timerUtil.finished(delay.getValue().longValue());
    }

    public void resetTimer() {
        timerUtil.reset();
    }

    public boolean shiftIsPressed() {
        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == 1
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == 1;
    }
}
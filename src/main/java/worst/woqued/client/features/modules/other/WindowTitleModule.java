package worst.woqued.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import worst.woqued.api.event.EventListener;
import worst.woqued.api.event.Listener;
import worst.woqued.api.event.events.client.TickEvent;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;

@ModuleRegister(name = "Window Title", category = Category.OTHER)
public class WindowTitleModule extends Module {
    @Getter private static final WindowTitleModule instance = new WindowTitleModule();
    private static final String CUSTOM_TITLE = "Woqued | 1.7.0";

    private WindowTitleModule() {
        setEnabled(true);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getWindow() != null) {
                long handle = mc.getWindow().getHandle();
                boolean isMinimized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;

                if (!isMinimized) {
                    if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                        GLFW.glfwSetWindowTitle(handle, CUSTOM_TITLE + " | " + mc.getSession().getUsername());
                    } else {
                        GLFW.glfwSetWindowTitle(handle, CUSTOM_TITLE);
                    }
                }
            }
        }));
        addEvents(tickEvent);
    }
}
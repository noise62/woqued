package sweetie.evaware.inject.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sweetie.evaware.api.event.events.other.ScreenEvent;
import sweetie.evaware.api.system.backend.KeyStorage;
import sweetie.evaware.api.system.backend.SharedClass;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.client.features.modules.other.AuctionHelperModule;
import sweetie.evaware.client.features.modules.other.MouseTweaksModule;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Shadow protected abstract boolean isPointOverSlot(Slot slotIn, double mouseX, double mouseY);
    @Shadow protected abstract void onMouseClick(Slot slotIn, int slotId, int mouseButton, SlotActionType type);

    @Unique private final TimerUtil timerUtil = new TimerUtil();

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ScreenEvent.ScreenEventData event = new ScreenEvent.ScreenEventData(this);
        ScreenEvent.getInstance().call(event);

        for (ButtonWidget button : event.buttons()) {
            this.addDrawableChild(button);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void drawScreenHook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        for (int i = 0; i < SharedClass.player().currentScreenHandler.slots.size(); ++i) {
            Slot slot = SharedClass.player().currentScreenHandler.slots.get(i);

            if (isPointOverSlot(slot, mouseX, mouseY) && slot.isEnabled()) {
                MouseTweaksModule mouseTweaks = MouseTweaksModule.getInstance();

                if (mouseTweaks.isEnabled() && shouldUse() && mouseIsHolding() && timerUtil.finished(mouseTweaks.delay.getValue().longValue())) {
                    onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
                    timerUtil.reset();
                }
            }
        }
    }

    @Unique
    private boolean shouldUse() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 340) || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 344);
    }

    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V", at = @At("TAIL"))
    protected void drawSlotHook(DrawContext context, Slot slot, CallbackInfo ci) {
        AuctionHelperModule module = AuctionHelperModule.getInstance();
        if (module.isEnabled()) module.onRenderChest(context, slot);
    }

    @Unique
    private boolean mouseIsHolding() {
        return KeyStorage.isPressed(-100 + GLFW.GLFW_MOUSE_BUTTON_1);
    }
}
